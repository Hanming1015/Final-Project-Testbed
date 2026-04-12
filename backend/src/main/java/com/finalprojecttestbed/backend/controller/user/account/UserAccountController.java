package com.finalprojecttestbed.backend.controller.user.account;


import com.finalprojecttestbed.backend.pojo.User;
import com.finalprojecttestbed.backend.service.user.account.InfoService;
import com.finalprojecttestbed.backend.service.user.account.LoginService;
import com.finalprojecttestbed.backend.service.user.account.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/account")
public class UserAccountController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private RegisterService registerService;

    @Autowired
    private InfoService infoService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        return ResponseEntity.ok(loginService.login(username, password));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        System.out.println(body);
        String username = body.get("username");
        String password = body.get("password");

        User user = registerService.register(username, password);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/info")
    public ResponseEntity<?> getInfo() {
        return ResponseEntity.ok(infoService.getInfo());
    }
}
