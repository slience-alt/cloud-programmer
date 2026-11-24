package com.cloud.programmer.user.migration;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cloud.programmer.user.entity.UserEntity;
import com.cloud.programmer.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("password-migrate")
@RequiredArgsConstructor
public class PasswordMigrationRunner implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        for (UserEntity user : userMapper.selectList(null)) {
            String password = user.getPassword();

            if (password == null || password.trim().isEmpty()) {
                continue;
            }

            // 已经是 BCrypt 密文则跳过
            if (password.startsWith("$2a$")
                    || password.startsWith("$2b$")
                    || password.startsWith("$2y$")) {
                continue;
            }

            String encodedPassword = passwordEncoder.encode(password);

            userMapper.update(
                    null,
                    new UpdateWrapper<UserEntity>()
                            .eq("id", user.getId())
                            .set("password", encodedPassword)
            );
        }
    }
}