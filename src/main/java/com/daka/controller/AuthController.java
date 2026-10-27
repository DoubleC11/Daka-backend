package com.daka.controller;

import com.daka.common.Result;
import com.daka.dto.AuthDto;
import com.daka.entity.User;
import com.daka.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody AuthDto.RegisterReq req) {
        return Result.ok(authService.register(req));
    }

    @PostMapping("/login")
    public Result<AuthDto.LoginResp> login(@Valid @RequestBody AuthDto.LoginReq req) {
        return Result.ok(authService.login(req));
    }

    @GetMapping("/me")
    public Result<User> me() {
        return Result.ok(authService.currentUser());
    }
}
