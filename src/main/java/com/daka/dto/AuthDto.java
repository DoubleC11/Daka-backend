package com.daka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

    public record RegisterReq(
            @NotBlank(message = "用户名不能为空")
            @Size(min = 3, max = 20, message = "用户名长度需在 3-20 之间")
            String username,

            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度需在 6-32 之间")
            String password,

            String nickname
    ) {
    }

    public record LoginReq(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {
    }

    public record LoginResp(String token, Object user) {
    }
}
