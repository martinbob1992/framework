package com.marsh.auth.service;

import com.marsh.auth.api.ITokenService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.token.TokenService;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.endpoint.CheckTokenEndpoint;
import org.springframework.security.oauth2.provider.token.TokenStore;

import java.util.Map;

/**
 * @author Marsh
 * @date 2022-02-24日 15:57
 */
@DubboService
@Slf4j
public class TokenServiceImpl implements ITokenService {

    @Autowired
    private CheckTokenEndpoint checkTokenEndpoint;
    @Autowired
    private TokenStore tokenStore;
    
    @Override
    public boolean checkToken(String token) {
        //OAuth2Authentication oAuth2Authentication = tokenStore.readAuthentication(token);
        try{
            Map<String, ?> stringMap = checkTokenEndpoint.checkToken(token);
            return true;
        } catch (Exception e){
            log.error("check token error!",e);
            return false;
        }


    }
}
