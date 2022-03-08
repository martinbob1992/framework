package com.marsh.auth.api;

/**
 * @author jitwxs
 * @date 2022年02月24日 15:54
 */
public interface ITokenService {

    boolean checkToken(String token);
}
