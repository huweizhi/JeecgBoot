package com.xinosoft.api.customer;

import org.jeecg.common.system.vo.LoginUser;

public interface CustomerApi {

    LoginUser getUserByUserId(String userId);

    /**
     * 生成密码
     * @param userId 用户ID
     * @param secret
     * @return 密码
     */
    String generatePassword(String userId, String secret);

}
