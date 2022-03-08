package com.marsh.auth.constant;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author marsh
 * @date 2020/6/16 14:27
 */
public enum AuthenticationEndpointEnum {

    // B端商户账户密码认证
    USER_PASSWD_TOKEN_URL("/auth/pc/passwd/token"),
    // 微信公众号获取token地址,目前用于C端
    WX_MP_TOKEN_URL("/auth/wx/mp/token");

    AuthenticationEndpointEnum(String endpointUrl){
        this.endpointUrl = endpointUrl;
    }

    private final String endpointUrl;

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public static List<String> getAllEndpointUrls(){
        return Arrays.stream(AuthenticationEndpointEnum.values()).map(AuthenticationEndpointEnum::getEndpointUrl).collect(Collectors.toList());
    }
}
