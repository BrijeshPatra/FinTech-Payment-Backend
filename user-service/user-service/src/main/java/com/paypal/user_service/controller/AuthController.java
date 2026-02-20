package com.paypal.user_service.controller;

import com.paypal.user_service.dto.LoginRequest;
import com.paypal.user_service.dto.SignupRequest;
import com.paypal.user_service.entity.User;
import com.paypal.user_service.repository.UserRepository;
import com.paypal.user_service.service.AuthService;
import com.paypal.user_service.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository userRepository;

    //using password encoder to encode the password
    private final PasswordEncoder passwordEncoder;

    //using jwt util because we have to validate the token and do authentication
    private final JwtUtil jwtUtil;

    private final AuthService authService;

    public AuthController(UserRepository userRepository,PasswordEncoder passwordEncoder
    ,JwtUtil jwtUtil,AuthService authService){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.jwtUtil=jwtUtil;
        this.authService=authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?>signUp(@RequestBody SignupRequest signupRequest) {
        authService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered successfully");
    }
    @PostMapping("/login")
    public ResponseEntity<?>login(@RequestBody LoginRequest loginRequest){
      authService.login(loginRequest);
      return ResponseEntity.ok(authService.login(loginRequest));
  }

}
