/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */
package com.marsh.auth.handler;

import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsh.auth.config.UAAAuthorizationServerConfigurerAdapter;
import com.marsh.common.response.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 登陆成功返回token和用户基本信息
 * @author marsh
 * @date 2018/11/7 17:34
 **/
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
	private static Logger log = LoggerFactory.getLogger(LoginSuccessHandler.class);
	private ObjectMapper objectMapper;
	private AuthorizationServerTokenServices authorizationServerTokenServices;
	private ClientDetailsService clientDetailsService;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

		try {
			HashMap<String,Object> data = new HashMap<String,Object>();
			List<String> scope = new ArrayList<String>();
			if (authentication instanceof UsernamePasswordAuthenticationToken){
				UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken)authentication;
				data.put("user",token.getDetails());
				scope.add("pc");
			}
			ClientDetails clientDetails = clientDetailsService.loadClientByClientId(UAAAuthorizationServerConfigurerAdapter.CLIENT_DETAILS_ID);
			TokenRequest tokenRequest = new TokenRequest(MapUtil.newHashMap(), UAAAuthorizationServerConfigurerAdapter.CLIENT_DETAILS_ID, scope, "authorization_code");

			//校验scope
			OAuth2Request oAuth2Request = tokenRequest.createOAuth2Request(clientDetails);
			OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);
			//authorizationServerTokenServices.getAccessToken(oAuth2Authentication);
			//authorizationServerTokenServices.refreshAccessToken("",tokenRequest)
			OAuth2AccessToken oAuth2AccessToken = authorizationServerTokenServices.createAccessToken(oAuth2Authentication);
			log.debug("获取token成功:{}", oAuth2AccessToken.getValue());
			data.put("token",oAuth2AccessToken);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write(objectMapper.writeValueAsString(R.ok(data)));
		} catch (IOException e) {
			throw new BadCredentialsException(
				"获取token失败",e);
		}
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public AuthorizationServerTokenServices getAuthorizationServerTokenServices() {
		return authorizationServerTokenServices;
	}

	public void setAuthorizationServerTokenServices(AuthorizationServerTokenServices authorizationServerTokenServices) {
		this.authorizationServerTokenServices = authorizationServerTokenServices;
	}

	public ClientDetailsService getClientDetailsService() {
		return clientDetailsService;
	}

	public void setClientDetailsService(ClientDetailsService clientDetailsService) {
		this.clientDetailsService = clientDetailsService;
	}
}
