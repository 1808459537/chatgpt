package com.zht.chatgptapi.interfaces;

import cn.bugstack.chatgpt.common.Constants;
import cn.bugstack.chatgpt.domain.chat.ChatCompletionRequest;
import cn.bugstack.chatgpt.domain.chat.ChatCompletionResponse;
import cn.bugstack.chatgpt.domain.chat.Message;
import cn.bugstack.chatgpt.session.Configuration;
import cn.bugstack.chatgpt.session.OpenAiSession;
import cn.bugstack.chatgpt.session.OpenAiSessionFactory;
import cn.bugstack.chatgpt.session.defaults.DefaultOpenAiSessionFactory;
import com.zht.chatgptapi.application.IWeiXinValidateService;
import com.zht.chatgptapi.domain.receive.model.MessageTextEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.zht.chatgptapi.infrastructure.util.XmlUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


@RestController
@RequestMapping("/wx/portal/{appid}")
public class WeiXinPortalController  {
    //openAi的SDK包
    private OpenAiSession openAiSession;

    private Logger logger = LoggerFactory.getLogger(WeiXinPortalController.class);

    @Value("${wx.config.originalid:gh_b60b3276fd51}")
    private String originalId;

    @Resource
    private IWeiXinValidateService weiXinValidateService;

    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    private Map<String, String> chatGPTMap = new ConcurrentHashMap<>();

    public WeiXinPortalController() {
        Configuration configuration = new Configuration();
        configuration.setApiHost("https://api1.openai-proxy.com/");
        configuration.setApiKey("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");
        OpenAiSessionFactory factory = new DefaultOpenAiSessionFactory(configuration);
        this.openAiSession = factory.openSession();
    }

    @GetMapping(produces = "text/plain;charset=utf-8")
    public String validate(@PathVariable String appid , @RequestParam String signature , @RequestParam String timestamp, @RequestParam String nonce , @RequestParam String echostr){
        try{
            logger.info("微信公众号验签信息{}开始 [{}, {}, {}, {}]", appid, signature, timestamp, nonce, echostr);
            if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
                throw new IllegalArgumentException("请求参数非法，请核实!");
            }

            boolean check = weiXinValidateService.checkSign(signature, timestamp, nonce);
            logger.info("微信公众号验签信息{}完成 check：{}", appid, check);
            if (!check) {
                return null;
            }

            return echostr;
        }catch (Exception e){
            logger.error("微信公众号验签信息{}失败 [{}, {}, {}, {}]", appid, signature, timestamp, nonce, echostr, e);
            return null;
        }
}

    /**
     * 此处是处理微信服务器的消息转发的
     */
    @PostMapping(produces = "application/xml; charset=UTF-8")
    public String post(@PathVariable String appid,
                       @RequestBody String requestBody,
                       @RequestParam("signature") String signature,
                       @RequestParam("timestamp") String timestamp,
                       @RequestParam("nonce") String nonce,
                       @RequestParam("openid") String openid,
                       @RequestParam(name = "encrypt_type", required = false) String encType,
                       @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        try {
            logger.info("接收微信公众号信息请求{}开始 {}", openid, requestBody);
            MessageTextEntity message = XmlUtil.xmlToBean(requestBody, MessageTextEntity.class);
            // 异步任务
//            if (chatGPTMap.get(message.getContent().trim()) == null || "NULL".equals(chatGPTMap.get(message.getContent().trim()))) {
//                // 反馈信息[文本]
//                MessageTextEntity res = new MessageTextEntity();
//                res.setToUserName(openid);
//                res.setFromUserName(originalId);
//                res.setCreateTime(String.valueOf(System.currentTimeMillis() / 1000L));
//                res.setMsgType("text");
//                res.setContent("消息处理中，请再回复我一句【" + message.getContent().trim() + "】");
//
//                if (chatGPTMap.get(message.getContent().trim()) == null) {
//                    doChatGPTTask(message.getContent().trim());
//                }
//
//                return XmlUtil.beanToXml(res);
//            }
            doChatGPTTask(message.getContent().trim());
            // 反馈信息[文本]
            MessageTextEntity res = new MessageTextEntity();
            res.setToUserName(openid);
            res.setFromUserName(originalId);
            res.setCreateTime(String.valueOf(System.currentTimeMillis() / 1000L));
            res.setMsgType("text");
            res.setContent(chatGPTMap.get(message.getContent().trim()));
            String result = XmlUtil.beanToXml(res);
            logger.info("接收微信公众号信息请求{}完成 {}", openid, result);
            chatGPTMap.remove(message.getContent().trim());
            return result;
        } catch (Exception e) {
            logger.error("接收微信公众号信息请求{}失败 {}", openid, requestBody, e);
            return "";
        }
    }

    @Deprecated
    public void doChatGPTTask(String content) {
        chatGPTMap.put(content, "NULL");
        taskExecutor.execute(() -> {
            // OpenAI 请求
            // 1. 创建参数
            ChatCompletionRequest chatCompletion = ChatCompletionRequest
                    .builder()
                    .messages(Collections.singletonList(Message.builder().role(Constants.Role.USER).content(content).build()))
                    .model(ChatCompletionRequest.Model.GPT_3_5_TURBO.getCode())
                    .build();


            // 2. 发起请求
            ChatCompletionResponse chatCompletionResponse = openAiSession.completions(chatCompletion);
            // 3. 解析结果
            StringBuilder messages = new StringBuilder();
            chatCompletionResponse.getChoices().forEach(e -> {
                messages.append(e.getMessage().getContent());
            });

            chatGPTMap.put(content, messages.toString());
        });
        while (true){
            if(chatGPTMap.get(content) !="NULL" ) break;
        }
    }

    public void doChatGPTTask02(String content) throws Exception {
        chatGPTMap.put(content, "NULL");
        ChatCompletionRequest chatCompletion = ChatCompletionRequest
                .builder()
                .messages(Collections.singletonList(Message.builder().role(Constants.Role.USER).content(content).build()))
                .model(ChatCompletionRequest.Model.GPT_3_5_TURBO.getCode())
                .stream(true)
                .build();
        // 2. 发起请求
       CompletableFuture<String> chatCompletions = openAiSession.chatCompletions(chatCompletion);
        // 3. 解析结果
        String message = chatCompletions.get();
        chatGPTMap.put(content, message);
    }
}
