package org.xtinastudio.com.security;


import org.xtinastudio.com.security.model.JwtAuthenticationResponse;
import org.xtinastudio.com.security.model.SignInRequest;

public interface AuthenticationService {

    JwtAuthenticationResponse authenticate(SignInRequest request);
}