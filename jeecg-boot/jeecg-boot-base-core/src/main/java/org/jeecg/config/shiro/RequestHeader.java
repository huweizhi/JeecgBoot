package org.jeecg.config.shiro;

import lombok.Data;
import org.jeecg.common.system.vo.LoginUser;

/**
 * 请求Header
 */
@Data
public class RequestHeader {
    private String requestId;
    private String token;
    private LoginUser loginUser;
}
