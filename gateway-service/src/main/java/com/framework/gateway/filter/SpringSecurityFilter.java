package com.framework.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.marsh.auth.api.ITokenService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * @author Marsh
 * @date 2022-02-23日 16:34
 */
@Slf4j
@Component
public class SpringSecurityFilter implements GlobalFilter, Ordered {

    @DubboReference
    private ITokenService tokenService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestUrl = exchange.getRequest().getPath().value();
        AntPathMatcher pathMatcher = new AntPathMatcher();
        //1 放行所有auth服务的请求
        if (pathMatcher.match("/api/auth/**", requestUrl)) {
            return chain.filter(exchange);
        }
        //2 检查token是否存在
        String token = getToken(exchange);
        if (StrUtil.isBlank(token)) {
            return noTokenMono(exchange);
        }
        //判断用户凭证是否存在
        if (!tokenService.checkToken(token)){
            return invalidTokenMono(exchange);
        }

        ServerHttpRequest tokenRequest = exchange.getRequest().mutate().header("json-token", token).build();
        ServerWebExchange build = exchange.mutate().request(tokenRequest).build();
        return chain.filter(exchange);
    }

    /**
     * 无效的token
     */
    private Mono<Void> invalidTokenMono(ServerWebExchange exchange) {
        JSONObject json = new JSONObject();
        json.put("status", HttpStatus.UNAUTHORIZED.value());
        json.put("data", "无效的token");
        return buildReturnMono(json, exchange);
    }

    private Mono<Void> noTokenMono(ServerWebExchange exchange) {
        JSONObject json = new JSONObject();
        json.put("status", HttpStatus.UNAUTHORIZED.value());
        json.put("data", "没有token");
        return buildReturnMono(json, exchange);
    }

    private Mono<Void> buildReturnMono(JSONObject json, ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        byte[] bits = json.toJSONString().getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bits);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        //指定编码，否则在浏览器中会中文乱码
        response.getHeaders().add("Content-Type", "text/plain;charset=UTF-8");
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 获取token
     */
    public String getToken(ServerWebExchange exchange) {
        String tokenStr = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (StrUtil.isBlank(tokenStr)) {
            return null;
        }
        String token = tokenStr.split(" ")[1];
        if (StrUtil.isBlank(token)) {
            return null;
        }
        return token;
    }

    /**
     *这个数值越小，越先执行该过滤器
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
