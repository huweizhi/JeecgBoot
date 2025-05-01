package com.xinosoft.config.shiro;

import com.xinosoft.enums.UserTypeEnum;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.realm.Realm;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.config.shiro.JwtToken;
import org.jeecg.config.shiro.ShiroRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

/**
 * 当配置了多个Realm时，我们通常使用的认证器是shiro自带的org.apache.shiro.authc.pam.ModularRealmAuthenticator，其中决定使用的Realm的是doAuthenticate()方法
 * 自定义Authenticator
 * 注意，当需要分别定义处理用户、商家、后台用户验证的Realm时
 */
public class UserModularRealmAuthenticator extends ModularRealmAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(UserModularRealmAuthenticator.class);

    @Override
    protected AuthenticationInfo doAuthenticate(AuthenticationToken authenticationToken)
            throws AuthenticationException {

        logger.debug("UserModularRealmAuthenticator:method doAuthenticate() execute ");

        if (authenticationToken == null) {
            throw new AuthenticationException("No token to authenticate");
        }

        JwtToken userToken = (JwtToken)authenticationToken;

        // 判断getRealms()是否返回为空
        assertRealmsConfigured();

        Optional<String> tokenOptional = Optional.of(userToken).map(JwtToken::getPrincipal).map(String::valueOf);

        if (tokenOptional.isEmpty()) {
            throw new AuthenticationException("No token to authenticate");
        }

        String token = tokenOptional.get();

        // 登录类型
        Optional<LoginUser> loginUserOptional = JwtUtil.getLoginUserByToken(token);

        if (loginUserOptional.isEmpty()) {
            throw new AuthenticationException();
        }

        LoginUser loginUser = loginUserOptional.get();

        // 所有Realm
        Collection<Realm> realms = getRealms();

        // 登录类型对应的所有Realm
        // List<Realm> typeRealms = new ArrayList<>();
//        for (Realm realm : realms) {
//            if (realm?.getName()?.contains(loginType))
//            typeRealms.add(realm);
//        }

        logger.debug("user type: " + loginUser.getUserType());

        Optional<Realm> realmOptional = null;

        if (loginUser.getUserType() == null || UserTypeEnum.后台用户.getValue().equals(loginUser.getUserType())) {
            realmOptional =  realms.stream().filter(r -> r instanceof ShiroRealm).findFirst();
        } else if (UserTypeEnum.商户.getValue().equals(loginUser.getUserType())) {
            realmOptional =  realms.stream().filter(r -> r instanceof MerchantUserRealm).findFirst();
        } else if (UserTypeEnum.客户.getValue().equals(loginUser.getUserType())) {
            realmOptional =  realms.stream().filter(r -> r instanceof CustomerUserRealm).findFirst();
        }

        if (realmOptional.isEmpty()) {
            throw new AuthenticationException("没有匹配的验证领域");
        }

       // typeRealms.add(realmOptional.get());

        logger.debug("realmOptional: " + realmOptional.get().getClass().getName());

        return doSingleRealmAuthentication(realmOptional.get(), userToken);

//        // 判断是单Realm还是多Realm
//        if (typeRealms.size() == 1){
//            logger.info("doSingleRealmAuthentication() execute ");
//            return doSingleRealmAuthentication(typeRealms.get(0), userToken);
//        } else {
//            logger.info("doMultiRealmAuthentication() execute ");
//            return doMultiRealmAuthentication(typeRealms, userToken);
//        }

    }
}

