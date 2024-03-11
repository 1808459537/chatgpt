package com.zht.chatgptapi.domain.validate;

import com.zht.chatgptapi.application.IWeiXinValidateService;
import com.zht.chatgptapi.infrastructure.util.sdk.SignatureUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
    @Service
    public class WeiXinValidateServiceImpl implements IWeiXinValidateService {

        @Value("${wx.config.token}")
        private String token;

        @Override
        public boolean checkSign(String signature, String timestamp, String nonce) {
            return SignatureUtil.check(token, signature, timestamp, nonce);
        }

    }
