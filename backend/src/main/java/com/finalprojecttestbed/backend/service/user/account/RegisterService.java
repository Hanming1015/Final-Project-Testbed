package com.finalprojecttestbed.backend.service.user.account;

import com.finalprojecttestbed.backend.pojo.User;

public interface RegisterService {
    User register(String username, String password);
}

