package com.marsh.auth.config;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.InMemoryClientDetailsService;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务器配置
 * 
 * @author marsh
 * @date 2018-10-21
 *
 */
@Configuration
@EnableAuthorizationServer
public class UAAAuthorizationServerConfigurerAdapter extends AuthorizationServerConfigurerAdapter {

	public static final String CLIENT_DETAILS_ID = "auth-service";

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	@Bean
	public TokenStore redisTokenStore() {
		RedisTokenStore tokenStore = new RedisTokenStore(redisConnectionFactory);
		return tokenStore;
	}


	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		clients.withClientDetails(clientDetails());
	}

	@Bean
	public ClientDetailsService clientDetails() {
		//目前仅有我们自己在使用登录系统，直接用内存模式即可，如果后续有其他机构认证了在使用JdbcClientDetailsService或者自己封装ReidsClientDetailsService
		InMemoryClientDetailsService memoryClientDetailsService = new InMemoryClientDetailsService();
		Map<String, BaseClientDetails> clientDetailsStore = new HashMap<>();
		BaseClientDetails client = new BaseClientDetails();
		//用于唯一标识每一个客户端(client)；注册时必须填写(也可以服务端自动生成)，这个字段是必须的，实际应用也有叫app_key
		client.setClientId(CLIENT_DETAILS_ID);
		//注册填写或者服务端自动生成，实际应用也有叫app_secret, 必须要有前缀代表加密方式
		client.setClientSecret(passwordEncoder.encode("123456"));
		//指定client的权限范围，比如读写权限，比如移动端还是web端权限
		client.setScope(Arrays.asList("all"));
		//不能为空，用逗号分隔
		//客户端能访问的资源id集合，注册客户端时，根据实际需要可选择资源id，也可以根据不同的额注册流程，赋予对应的额资源id
		//penglai.setResourceIds();
		//授权码模式:authorization_code,密码模式:password,刷新token: refresh_token, 隐式模式: implicit: 客户端模式: client_credentials。支持多个用逗号分隔
		client.setAuthorizedGrantTypes(Lists.newArrayList("password","authorization_code","refresh_token"));

		clientDetailsStore.put(client.getClientId(),client);
		memoryClientDetailsService.setClientDetailsStore(clientDetailsStore);
		return memoryClientDetailsService;
	}

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		// token增强配置
		TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
		endpoints.tokenStore(redisTokenStore()).tokenEnhancer(tokenEnhancerChain)
				.authenticationManager(authenticationManager).reuseRefreshTokens(false)
				.userDetailsService(userDetailsService);
	}
	
	/**
     * <p>注意，自定义TokenServices的时候，需要设置@Primary，否则报错，</p>
     * @return
     */
    @Primary
    @Bean
    public AuthorizationServerTokenServices defaultTokenServices(){
        DefaultTokenServices tokenServices = new DefaultTokenServices();
        tokenServices.setTokenStore(redisTokenStore());
        tokenServices.setSupportRefreshToken(true);
        tokenServices.setClientDetailsService(clientDetails());
        tokenServices.setAccessTokenValiditySeconds(60*60*24*7); // token有效期自定义设置，默认7天
        tokenServices.setRefreshTokenValiditySeconds(60 * 60 * 24 * 30);//默认30天
        return tokenServices;
    }


	@Override
	public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
		// 开启/oauth/token_key验证端口无权限访问
		security.tokenKeyAccess("permitAll()");
		// 开启/oauth/check_token验证端口认证权限访问
		//security.checkTokenAccess("isAuthenticated()");
		security.checkTokenAccess("permitAll()");
		security.allowFormAuthenticationForClients();
	}
}
