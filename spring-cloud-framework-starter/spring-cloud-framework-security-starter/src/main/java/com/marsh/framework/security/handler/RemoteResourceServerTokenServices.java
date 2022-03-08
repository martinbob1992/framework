package com.marsh.framework.security.handler;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

/**
 * @author Marsh
 * @date 2022-03-07日 14:53
 */
public class RemoteResourceServerTokenServices implements ResourceServerTokenServices {

    @Override
    public OAuth2Authentication loadAuthentication(String s) throws AuthenticationException, InvalidTokenException {
        return null;
    }

    @Override
    public OAuth2AccessToken readAccessToken(String s) {
        return null;
    }
}
