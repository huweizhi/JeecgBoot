package com.xinosoft.config.mybatis;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
@EnableScheduling
public class MyBatisPlusConfig implements SchedulingConfigurer {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${mybatis-plus.global-config.db-config.init-worker-id-from-redis:false}")
    private boolean initWorkerIdFromRedis;

    private String WORKER_ID_MAP_KEY = "snowflake:worker_id_map";
    private String WORKER_ID_LOCK_KEY = "snowflake:worker_id_lock";
    private static final int MAX_WORKER_ID = 31;
    private static final int MIN_WORKER_ID = 0;

    @Autowired
    private RedissonClient redissonClient;

    private long currentWorkerId = 1;      // 机器ID（0-31）
    private final long currentDataCenterId = 1;  // 数据中心ID（0-31）
    private volatile boolean workerIdAssigned = false; // 标记是否已分配 workerId

    @Bean
    public IdentifierGenerator identifierGenerator() {
        return entity -> {
            // log.debug("生成雪花ID，workerId: {}, dataCenterId: {}", currentWorkerId, currentDataCenterId);
            IdWorker.initSequence(currentWorkerId, currentDataCenterId);
            return IdWorker.getId();
        };
    }

    @PostConstruct
    public void initWorkerId() {

        if (!initWorkerIdFromRedis) {
            this.currentWorkerId = 0;
            this.workerIdAssigned = true;
            log.info("未启用从Redis初始化雪花算法 workerId，使用默认 workerId: {}", this.currentWorkerId);
            return;
        }

        WORKER_ID_MAP_KEY = applicationName + ":" + WORKER_ID_MAP_KEY;
        WORKER_ID_LOCK_KEY = applicationName + ":" + WORKER_ID_LOCK_KEY;

        RLock lock = null;
        try {
            // 获取分布式锁，等待时间3秒，锁租用时间10秒
            lock = this.tryLock(WORKER_ID_LOCK_KEY, 3, 10, TimeUnit.SECONDS);

            // 获取 workerIdMap
            /*
              redisson.getMap("key") 是Redisson提供的获取分布式Map代理对象的操作。
              其核心作用是建立客户端与Redis中名为"key"的Hash结构的关联，返回一个RMap实例（实现了java.util.Map接口）。
              此时，Redisson仅完成了本地代理对象的初始化，并未触发任何与Redis的网络通信或数据拉取动作
              只有当调用RMap实例的具体数据操作方法​（如entrySet()、keySet()、values()或遍历操作）时，Redisson才会向Redis发送命令获取数据
            */
            RMap<String, WorkerStatus> workerIdMap = redissonClient.getMap(WORKER_ID_MAP_KEY);

            // 如果 workerIdMap 不存在或为空，初始化并设置 0 为已使用
            if (workerIdMap.isEmpty()) {
                log.info("初始化 workerIdMap，设置 workerId 0 为已使用");
                initWorkerIdMap(workerIdMap);
                this.currentWorkerId = 0;
                LocalDateTime now = LocalDateTime.now();
                workerIdMap.put("0", new WorkerStatus(true, now, now));
            } else {
                // 从小到大查找第一个未使用的 workerId
                this.currentWorkerId = findAvailableWorkerId(workerIdMap);
                // 标记为已使用
                LocalDateTime now = LocalDateTime.now();
                workerIdMap.put(String.valueOf(this.currentWorkerId), new WorkerStatus(true, now, now));
            }
            this.workerIdAssigned = true; // 标记已成功分配
            log.info("分配的 workerId: {}", this.currentWorkerId);
        } catch (Exception e) {
            log.error("初始化雪花算法 workerId 失败", e);
            throw new RuntimeException("初始化雪花算法 workerId 失败", e);
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 初始化 workerIdMap，所有 workerId 设置为未使用状态
     */
    private void initWorkerIdMap(RMap<String, WorkerStatus> workerIdMap) {
        for (int i = MIN_WORKER_ID; i <= MAX_WORKER_ID; i++) {
            workerIdMap.put(String.valueOf(i), new WorkerStatus(false, null, null));
        }
    }

    /**
     * 查找第一个可用的 workerId
     */
    private long findAvailableWorkerId(RMap<String, WorkerStatus> workerIdMap) {
        Map<String, WorkerStatus> workerStatusMap = workerIdMap.readAllMap();

        // 先检查并释放心跳超时的 workerId

        LocalDateTime oneHourAgo = LocalDateTime.now().minusMinutes(1);

        Map<String, WorkerStatus> workerStatusMap2 = new HashMap<>();

        for (String workerId : workerStatusMap.keySet()) {

            WorkerStatus workerStatus = workerStatusMap.get(workerId);

            if (workerStatus == null || !workerStatus.isWorkerIdAssigned()) {
                continue;
            }
            // 检查心跳时间是否超过60分钟
            LocalDateTime heartbeatTime = workerStatus.getHeartbeatTime();
            if (heartbeatTime != null) {
                if (heartbeatTime.isBefore(oneHourAgo)) {
                    // 心跳超时，释放该 workerId
                    log.warn("发现心跳超时的 workerId:{}，释放该 workerId，状态: {}",workerId,  workerStatus);
                    workerStatus.setWorkerIdAssigned(false);
                    workerStatus.setAssignedTime(null);
                    workerStatus.setHeartbeatTime(null);
                    workerStatusMap2.put(workerId, workerStatus);
                }
            }
        }

        if (!workerStatusMap2.isEmpty()) {
            workerIdMap.putAll(workerStatusMap2);
            // 刷新本地缓存
            workerStatusMap.putAll(workerStatusMap2);
        }

        for (int i = MIN_WORKER_ID; i <= MAX_WORKER_ID; i++) {
            WorkerStatus status = workerStatusMap.get(String.valueOf(i));
            if (status == null || !status.isWorkerIdAssigned()) {
                log.info("找到可用的 workerId: {}", i);
                return i;
            }
        }
        // 如果所有 workerId 都被使用，抛出异常
        throw new RuntimeException("所有 workerId (0-31) 都已被使用，无法分配新的 workerId");
    }

    @Override
    public void configureTasks(@NotNull ScheduledTaskRegistrar registrar) {
        if (initWorkerIdFromRedis) {
            registrar.addFixedRateTask(this::heartbeat, 1 * 60 * 1000);
        }
    }

    /**
     * 定时心跳任务，每5分钟执行一次
     */
    public void heartbeat() {
        if (!workerIdAssigned) {
            log.warn("未分配 workerId，跳过心跳更新");
            return;
        }

        try {
            // 获取 workerIdMap
            RMap<String, WorkerStatus> workerIdMap = redissonClient.getMap(WORKER_ID_MAP_KEY);

            String workerIdKey = String.valueOf(this.currentWorkerId);
            log.info("发送心跳更新，workerId: {}", this.currentWorkerId);

            WorkerStatus currentStatus = workerIdMap.get(workerIdKey);
            if (currentStatus == null) {
                log.error("workerId {} 在 workerIdMap 中不存在，心跳更新失败", this.currentWorkerId);
                return;
            }

            if (!currentStatus.isWorkerIdAssigned()) {
                log.error("workerId {} 当前状态是未分配状态，心跳更新失败", this.currentWorkerId);
                return;
            }

            currentStatus.setHeartbeatTime(LocalDateTime.now());
            workerIdMap.put(String.valueOf(currentWorkerId), currentStatus);
            log.info("成功更新 workerId {} 心跳时间: {}", this.currentWorkerId, currentStatus.getHeartbeatTime());

        } catch (Exception e) {
            log.error("更新 workerId {} 心跳失败", this.currentWorkerId, e);
        }
    }

    @PreDestroy
    public void releaseWorkerId() {

        if (!initWorkerIdFromRedis) {
            log.info("未启用从Redis初始化雪花算法 workerId，跳过释放逻辑");
            return;
        }

        // 只有在成功分配了 workerId 的情况下才需要释放
        if (!workerIdAssigned) {
            log.info("未分配 workerId，无需释放");
            return;
        }

        RLock lock = null;
        try {
            // 获取分布式锁
            lock = this.tryLock(WORKER_ID_LOCK_KEY, 3, 10, TimeUnit.SECONDS);

            // 获取 workerIdMap
            RMap<String, WorkerStatus> workerIdMap = redissonClient.getMap(WORKER_ID_MAP_KEY);

            // 直接根据键释放 workerId，使用原子操作
            String workerIdKey = String.valueOf(this.currentWorkerId);
            WorkerStatus releasedStatus = workerIdMap.compute(workerIdKey, (key, status) -> {
                if (status != null && status.isWorkerIdAssigned()) {
                    // 设置为未分配状态
                    return new WorkerStatus(false, null, null);
                } else {
                    log.warn("workerId {} 当前状态异常，状态: {}", this.currentWorkerId, status);
                    return status; // 保持原状态
                }
            });

            if (releasedStatus != null && !releasedStatus.isWorkerIdAssigned()) {
                log.info("成功释放 workerId: {}", this.currentWorkerId);
            }

            this.workerIdAssigned = false;

        } catch (Exception e) {
            log.error("释放 workerId {} 失败", this.currentWorkerId, e);
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 尝试加锁
     * @param lockName 锁名称
     * @param waitTime 等待时间
     * @param leaseTime 租用时间
     * @param timeUnit 时间单位
     * @return 锁对象
     */
    public RLock tryLock(String lockName, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = this.redissonClient.getLock(lockName);

        boolean locked = false;

        try {
            locked = lock.tryLock(waitTime, leaseTime, timeUnit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!locked) {
            log.error("获取 {} 锁失败", lock.getName());
            throw new RuntimeException("系统繁忙,请稍后再试");
        }

        return lock;
    }

    /**
     * WorkerId 状态信息
     */
    public static class WorkerStatus implements Serializable {
        private static final long serialVersionUID = 1L;

        private boolean workerIdAssigned;  // 是否已分配
        private LocalDateTime assignedTime;  // 分配时间
        private LocalDateTime heartbeatTime; // 心跳时间

        public WorkerStatus() {}

        public WorkerStatus(boolean workerIdAssigned, LocalDateTime assignedTime, LocalDateTime heartbeatTime) {
            this.workerIdAssigned = workerIdAssigned;
            this.assignedTime = assignedTime;
            this.heartbeatTime = heartbeatTime;
        }

        // Getters and Setters
        public boolean isWorkerIdAssigned() {
            return workerIdAssigned;
        }

        public void setWorkerIdAssigned(boolean workerIdAssigned) {
            this.workerIdAssigned = workerIdAssigned;
        }

        public LocalDateTime getAssignedTime() {
            return assignedTime;
        }

        public void setAssignedTime(LocalDateTime assignedTime) {
            this.assignedTime = assignedTime;
        }

        public LocalDateTime getHeartbeatTime() {
            return heartbeatTime;
        }

        public void setHeartbeatTime(LocalDateTime heartbeatTime) {
            this.heartbeatTime = heartbeatTime;
        }

        @Override
        public String toString() {
            return "WorkerStatus{" +
                    "workerIdAssigned=" + workerIdAssigned +
                    ", assignedTime=" + assignedTime +
                    ", heartbeatTime=" + heartbeatTime +
                    '}';
        }
    }

}
