package com.cloud.programmer.user.controller;

import com.cloud.programmer.security.context.SecurityUtils;
import com.cloud.programmer.user.entity.UserEntity;
import com.cloud.programmer.user.model.vo.LoginVO;
import com.cloud.programmer.user.model.vo.RegisterVO;
import com.cloud.programmer.user.model.vo.UserInfoVO;
import com.cloud.programmer.user.model.vo.UserVO;
import com.cloud.programmer.user.service.JwtTokenService;
import com.cloud.programmer.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginVO> login(@RequestBody LoginVO loginVO) {
        return ResponseEntity.ok(userService.login(loginVO));
    }

    @PostMapping("/register")
    public ResponseEntity<LoginVO> register(@RequestBody RegisterVO registerVO) {
        return ResponseEntity.ok(userService.register(registerVO));
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoVO> currentUser() {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserInfoVO user = userService.getUserById(currentUserId);
        return ResponseEntity.ok(user);
    }
}