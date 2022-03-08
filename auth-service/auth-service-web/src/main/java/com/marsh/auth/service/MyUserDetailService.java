package com.marsh.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marsh
 * @date 2022-02-23日 14:34
 */
@Component
public class MyUserDetailService implements UserDetailsService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String account) throws UsernameNotFoundException {
        // 正常通过数据库查询这个用户是否存在，这里简单判断下账户是否包含admin
        if (!account.contains("admin")){
            throw new UsernameNotFoundException("该账户信息不存在!");
        }
        // 正常情况下密码从数据库中查出来，这里写死123456
        String password = passwordEncoder.encode("123456");
        // 这里不加载任何权限码，如需要可通过userId自行进行查询
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        if ("admin".equals(account)){
            SimpleGrantedAuthority adminAuthority = new SimpleGrantedAuthority("admin");
            authorities.add(adminAuthority);
        }

        User user = new User(account, password,true, true, true, true, authorities);
        return user;
    }
}
