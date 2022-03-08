package com.marsh.auth.authentication.passwd.provider;

import com.marsh.auth.service.MyUserDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * B端商户通过账号密码模式进行授权认证处理器
 * @author marsh
 * @date 2020/6/16 10:48
 */
@Slf4j
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {

    private PasswordEncoder passwordEncoder;
    private MyUserDetailService userDetailService;
    //是否隐藏密账号找不到异常
    protected boolean hideUserNotFoundExceptions = true;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (userDetailService == null){
            throw new RuntimeException(
                    "密码模式验证失败,未配置UserDetailService!");
        }

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
        String account = token.getPrincipal().toString();
        UserDetails userDetails = null;
        try {
            userDetails = userDetailService.loadUserByUsername(account);
        } catch (UsernameNotFoundException usernameNotFoundException){
            if (hideUserNotFoundExceptions) {
                throw new BadCredentialsException("账号或密码错误");
            } else {
                throw usernameNotFoundException;
            }
        }
        additionalAuthenticationChecks(userDetails,token);
        UsernamePasswordAuthenticationToken successToken = new UsernamePasswordAuthenticationToken(userDetails.getUsername(),"",userDetails.getAuthorities());
        successToken.setDetails(userDetails);
        return successToken;
    }

    /**
     * 校验用户密码是否正确
     * @param userDetails
     * @param authentication
     * @throws AuthenticationException
     */
    protected void additionalAuthenticationChecks(UserDetails userDetails,
                                                  UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {

        String presentedPassword = authentication.getCredentials().toString();

        if (!passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
            throw new BadCredentialsException("账号或密码错误");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.getName().equals(authentication.getName());
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public MyUserDetailService getUserDetailsService() {
        return userDetailService;
    }

    public void setUserDetailsService(MyUserDetailService userDetailService) {
        this.userDetailService = userDetailService;
    }

    public boolean isHideUserNotFoundExceptions() {
        return hideUserNotFoundExceptions;
    }

    public void setHideUserNotFoundExceptions(boolean hideUserNotFoundExceptions) {
        this.hideUserNotFoundExceptions = hideUserNotFoundExceptions;
    }

}
