package com.marsh.user.controller;

import com.marsh.common.response.R;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Marsh
 * @date 2022-02-24日 17:40
 */
@RestController
public class UserController {

    @GetMapping("/echo")
    public R<String> echo(String echo){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return R.ok("hello "+echo);
    }

}
