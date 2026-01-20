package com.yantai.superinventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yantai.superinventory.model.User;
import com.yantai.superinventory.repository.UserRepository;
import com.yantai.superinventory.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 微信认证服务
 * 处理微信OAuth2.0认证流程
 */
@Service
public class WeChatAuthService {
    
    @Value("${wechat.appid}")
    private String appId;
    
    @Value("${wechat.appsecret}")
    private String appSecret;
    
    @Value("${wechat.redirect.uri}")
    private String redirectUri;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 生成微信授权URL
     * 
     * @param state 状态参数，用于防止CSRF攻击
     * @return 微信授权URL
     */
    public String generateAuthUrl(String state) {
        // 对redirect_uri进行URL编码
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        
        return String.format(
            "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_userinfo&state=%s#wechat_redirect",
            appId,
            encodedRedirectUri,
            state
        );
    }
    
    /**
     * 使用授权码换取access_token
     * 
     * @param code 微信返回的授权码
     * @return access_token响应
     * @throws Exception 如果请求失败
     */
    public Map<String, Object> getAccessToken(String code) throws Exception {
        String url = String.format(
            "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
            appId,
            appSecret,
            code
        );
        
        String response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        
        JsonNode jsonNode = objectMapper.readTree(response);
        
        // 检查是否有错误
        if (jsonNode.has("errcode")) {
            int errcode = jsonNode.get("errcode").asInt();
            String errmsg = jsonNode.get("errmsg").asText();
            throw new RuntimeException("微信API错误: " + errcode + " - " + errmsg);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("access_token", jsonNode.get("access_token").asText());
        result.put("openid", jsonNode.get("openid").asText());
        result.put("refresh_token", jsonNode.get("refresh_token").asText());
        result.put("expires_in", jsonNode.get("expires_in").asInt());
        
        if (jsonNode.has("unionid")) {
            result.put("unionid", jsonNode.get("unionid").asText());
        }
        
        return result;
    }
    
    /**
     * 获取微信用户信息
     * 
     * @param accessToken access_token
     * @param openid 用户openid
     * @return 用户信息
     * @throws Exception 如果请求失败
     */
    public Map<String, Object> getUserInfo(String accessToken, String openid) throws Exception {
        String url = String.format(
            "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN",
            accessToken,
            openid
        );
        
        String response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        
        JsonNode jsonNode = objectMapper.readTree(response);
        
        // 检查是否有错误
        if (jsonNode.has("errcode")) {
            int errcode = jsonNode.get("errcode").asInt();
            String errmsg = jsonNode.get("errmsg").asText();
            throw new RuntimeException("获取用户信息失败: " + errcode + " - " + errmsg);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("openid", jsonNode.get("openid").asText());
        result.put("nickname", jsonNode.get("nickname").asText());
        result.put("headimgurl", jsonNode.get("headimgurl").asText());
        
        if (jsonNode.has("unionid")) {
            result.put("unionid", jsonNode.get("unionid").asText());
        }
        
        return result;
    }
    
    /**
     * 处理微信登录回调
     * 
     * @param code 微信返回的授权码
     * @return JWT Token和用户信息
     * @throws Exception 如果处理失败
     */
    public Map<String, Object> handleWeChatCallback(String code) throws Exception {
        // 1. 使用code换取access_token
        Map<String, Object> tokenResponse = getAccessToken(code);
        String accessToken = (String) tokenResponse.get("access_token");
        String openid = (String) tokenResponse.get("openid");
        String unionid = (String) tokenResponse.get("unionid");
        
        // 2. 使用snsapi_userinfo获取用户详细信息
        String nickname;
        String avatarUrl;
        
        try {
            // 尝试获取用户信息
            Map<String, Object> userInfo = getUserInfo(accessToken, openid);
            nickname = (String) userInfo.get("nickname");
            avatarUrl = (String) userInfo.get("headimgurl");
            
            // 如果有unionid，更新它
            if (userInfo.containsKey("unionid")) {
                unionid = (String) userInfo.get("unionid");
            }
        } catch (Exception e) {
            // 如果获取失败，使用默认值
            System.err.println("获取微信用户信息失败，使用默认值: " + e.getMessage());
            nickname = "用户" + openid.substring(openid.length() - 6);
            avatarUrl = null;
        }
        
        // 3. 查找或创建用户
        User user = findOrCreateUser(openid, unionid, nickname, avatarUrl);
        
        // 4. 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        // 5. 生成JWT Token
        String token = jwtTokenProvider.generateToken(
            user.getId(),
            user.getWechatOpenid(),
            user.getNickname(),
            user.getAvatarUrl(),
            user.getRole()
        );
        
        // 6. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", convertUserToMap(user));
        
        return result;
    }
    
    /**
     * 查找或创建用户
     * 
     * @param openid 微信OpenID
     * @param unionid 微信UnionID
     * @param nickname 用户昵称
     * @param avatarUrl 用户头像URL
     * @return 用户对象
     */
    private User findOrCreateUser(String openid, String unionid, String nickname, String avatarUrl) {
        Optional<User> existingUser = userRepository.findByWechatOpenid(openid);
        
        if (existingUser.isPresent()) {
            // 更新用户信息
            User user = existingUser.get();
            user.setNickname(nickname);
            user.setAvatarUrl(avatarUrl);
            if (unionid != null) {
                user.setWechatUnionid(unionid);
            }
            return userRepository.save(user);
        } else {
            // 创建新用户
            User newUser = new User();
            newUser.setWechatOpenid(openid);
            newUser.setWechatUnionid(unionid);
            newUser.setNickname(nickname);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setRole("USER");
            newUser.setIsActive(true);
            return userRepository.save(newUser);
        }
    }
    
    /**
     * 将User对象转换为Map
     * 
     * @param user 用户对象
     * @return Map对象
     */
    private Map<String, Object> convertUserToMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId().toString());
        userMap.put("openid", user.getWechatOpenid());
        userMap.put("nickname", user.getNickname());
        userMap.put("avatarUrl", user.getAvatarUrl());
        userMap.put("role", user.getRole());
        userMap.put("createdAt", user.getCreatedAt().toString());
        if (user.getLastLoginAt() != null) {
            userMap.put("lastLoginAt", user.getLastLoginAt().toString());
        }
        return userMap;
    }
    
    /**
     * 根据Token获取用户信息
     * 
     * @param token JWT Token
     * @return 用户信息
     * @throws Exception 如果Token无效
     */
    public Map<String, Object> getUserByToken(String token) throws Exception {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("Token无效或已过期");
        }
        
        String openid = jwtTokenProvider.getOpenidFromToken(token);
        Optional<User> userOpt = userRepository.findByWechatOpenid(openid);
        
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        
        return convertUserToMap(userOpt.get());
    }
}
