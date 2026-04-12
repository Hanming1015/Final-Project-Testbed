package com.finalprojecttestbed.backend.service.impl.user.account;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finalprojecttestbed.backend.mapper.UserMapper;
import com.finalprojecttestbed.backend.pojo.User;
import com.finalprojecttestbed.backend.service.user.account.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegisterServiceImpl implements RegisterService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User register(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Username and password must not be empty");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User existing = userMapper.selectOne(queryWrapper);
        if (existing != null) {
            if (username.equals(existing.getUsername())) {
                throw new IllegalArgumentException("Username already exists");
            }
            throw new IllegalArgumentException("User already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));

        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }
}
