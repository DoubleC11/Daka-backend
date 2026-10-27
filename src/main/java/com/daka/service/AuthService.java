package com.daka.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.daka.common.BizException;
import com.daka.common.JwtUtil;
import com.daka.common.UserContext;
import com.daka.dto.AuthDto;
import com.daka.entity.User;
import com.daka.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private static final List<String> AVATAR_COLORS =
            List.of("sunset", "violet", "mint", "sky", "rose", "amber");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public User register(AuthDto.RegisterReq req) {
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, req.username()));
        if (exists != null && exists > 0) {
            throw new BizException("用户名已被占用");
        }

        User user = new User();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setNickname(req.nickname() == null || req.nickname().isBlank()
                ? req.username()
                : req.nickname().trim());
        user.setAvatarColor(AVATAR_COLORS.get(Math.abs(req.username().hashCode()) % AVATAR_COLORS.size()));
        userMapper.insert(user);

        user.setPassword(null);
        return user;
    }

    public AuthDto.LoginResp login(AuthDto.LoginReq req) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, req.username()));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BizException("用户名或密码不正确");
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        user.setPassword(null);
        return new AuthDto.LoginResp(token, user);
    }

    public User currentUser() {
        User user = userMapper.selectById(UserContext.require());
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        user.setPassword(null);
        return user;
    }
}
