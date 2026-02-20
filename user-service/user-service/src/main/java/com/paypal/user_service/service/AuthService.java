package com.paypal.user_service.service;

import com.paypal.user_service.dto.LoginRequest;
import com.paypal.user_service.dto.LoginResponse;
import com.paypal.user_service.dto.SignupRequest;

public interface AuthService {

    void signup(SignupRequest signupRequest);

    LoginResponse login(LoginRequest loginRequest);
}
