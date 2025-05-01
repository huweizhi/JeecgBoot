package com.xinosoft.config.tenant;


import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.xinosoft.api.tenant.SysTenantDTO;
import com.xinosoft.api.tenant.TenantServiceApi;
import com.xinosoft.config.mybatis.DynamicDataSource;
import com.xinosoft.config.mybatis.SchemaInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.config.sign.util.BodyReaderHttpServletRequestWrapper;
import org.jeecg.config.sign.util.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * 租户ID拦截器
 * @author huweizhi
 */
@Slf4j
public class TenantIdInterceptor implements HandlerInterceptor {

    @Lazy
    @Autowired
    @Qualifier("sysTenantServiceImpl")
    private TenantServiceApi tenantServiceApi;

    /**
     * 5分钟有效期
     */
    private final static long MAX_EXPIRE = 5 * 60;

    private Map<Integer, SysTenantDTO> tenantMap;

    private synchronized void loadTenant() {

        if (tenantMap != null && !tenantMap.isEmpty()) {
            return;
        }

        List<SysTenantDTO> tenantList = tenantServiceApi.queryEffectiveTenant();

        if (tenantList == null || tenantList.isEmpty()) {
            return;
        }

        tenantMap = new java.util.HashMap<>(tenantList.size());

        for (SysTenantDTO tenant : tenantList) {
            tenantMap.put(tenant.getId(), tenant);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("TenantId Interceptor request URI = " + request.getRequestURI());
        //对参数进行签名验证
        String tenantId = request.getHeader(CommonConstant.TENANT_ID);
        log.info("TenantId Interceptor tenantId = " + tenantId);

        if (tenantId != null) {
            if (tenantMap == null || tenantMap.isEmpty()) {
                loadTenant();
            }
            SysTenantDTO sysTenantDTO = tenantMap.get(Integer.parseInt(tenantId));
            SchemaInterceptor.setSchema(sysTenantDTO.getDbName());
            return true;
        }

        return true;

    }

    /**
     * 在整个请求结束之后被调用，也就是在DispatcherServlet 渲染了对应的视图之后执行（主要是用于进行资源清理工作）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        //清除线程变量
        SchemaInterceptor.clearSchema();
    }


}
