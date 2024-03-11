package com.zht.chatgptapi.application;

public interface IWeiXinValidateService {
    boolean checkSign(String signature, String timestamp, String nonce);
}
