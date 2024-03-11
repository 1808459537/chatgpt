package com.zht.chatgptapi.interfaces;

import com.zht.chatgptapi.ChatgptApiApplication;
import com.zht.chatgptapi.domain.security.service.JwtUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiAccessController {
    private Logger logger = LoggerFactory.getLogger(ApiAccessController.class);
    @GetMapping("/verify")
    public ResponseEntity<String> verify(String token) {
        logger.info("验证 token：{}", token);
        return ResponseEntity.status(HttpStatus.OK).body("verify success!");
    }


    @GetMapping("/success")
    public String success(){
        return "test success by xfg";
    }


    @GetMapping("/verifys")
    public ResponseEntity<String> verifys(String token) {
        logger.info("验证 token：{}", token);
        if ("success".equals(token)){
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/authorize")
    public ResponseEntity<Map<String ,String>> authorize(String username ,String password){
        //最后需要返回的东西
        Map<String ,String> map = new HashMap<>();
        // 模拟校验
        if(!"zht".equals(username) ||!"123".equals(password)){
            map.put("msg","失败");
            return ResponseEntity.ok(map);
        }

        JwtUtil jwtUtil = new JwtUtil();
        Map<String ,Object> chaim = new HashMap<>();
        chaim.put("username" ,username);
        String token = jwtUtil.encode(username ,5 * 60 * 1000 , chaim);
        map.put("msg" ,"授权成功");
        map.put("token",token);
        return ResponseEntity.ok(map);
    }
}
