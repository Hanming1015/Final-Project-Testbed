package com.finalprojecttestbed.backend.service.user.account;

import java.util.Map;

public interface LoginService {
    Map<String, Object> login(String username, String password);
}
