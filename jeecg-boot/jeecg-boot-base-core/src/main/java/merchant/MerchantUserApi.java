package merchant;

import org.jeecg.common.system.vo.LoginUser;

/**
 * @Description: 商家用户
 * @Author: jeecg-boot
 * @Date:   2023-07-09
 * @Version: V1.0
 */
public interface MerchantUserApi {

    final String CACHE_MERCHANT_USER_PREFIX = "cache:merchant_login_user";

    /**
     * 获取登陆用户
     * @param username 用户名
     * @return
     */
    public LoginUser getUserByName(String username);

}
