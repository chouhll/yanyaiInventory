package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.QRCodeState;
import com.yantai.superinventory.service.QRCodeService;
import com.yantai.superinventory.service.WeChatAuthService;
import com.yantai.superinventory.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

/**
 * 认证控制器
 * 处理用户登录、注册、Token验证等认证相关操作
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private WeChatAuthService weChatAuthService;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private QRCodeService qrcodeService;
    
    @Value("${wechat.token}")
    private String wechatToken;
    
    /**
     * 获取微信授权URL
     * GET /api/auth/wechat/login
     * 
     * @return 微信授权URL
     */
    @GetMapping("/wechat/login")
    public ResponseEntity<?> getWeChatAuthUrl() {
        try {
            String state = UUID.randomUUID().toString();
            String authUrl = weChatAuthService.generateAuthUrl(state);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "authUrl", authUrl,
                "state", state
            ));
            response.put("message", "获取授权URL成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取授权URL失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 处理微信回调
     * GET /api/auth/wechat/callback?code=xxx&state=xxx
     * 
     * @param code 微信返回的授权码
     * @param state 状态参数
     * @return 重定向到前端Dashboard，Token通过Cookie传递
     */
    @GetMapping("/wechat/callback")
    public ResponseEntity<?> handleWeChatCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state) {
        try {
            if (code == null || code.isEmpty()) {
                // 重定向到前端登录页并显示错误
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", "https://superinventory.cfapps.us10-001.hana.ondemand.com/login?error=no_code")
                        .build();
            }
            
            // 处理微信回调
            Map<String, Object> result = weChatAuthService.handleWeChatCallback(code);
            String token = (String) result.get("token");
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) result.get("user");
            
            // 检查是否是二维码登录请求
            if (state != null && !state.isEmpty()) {
                QRCodeState qrcodeState = qrcodeService.getQRCodeState(state);
                if (qrcodeState != null && !qrcodeState.isExpired()) {
                    // 是二维码登录，更新state状态
                    qrcodeService.markQRCodeSuccess(state, token);
                    
                    // 返回成功提示页面（微信内显示）
                    String html = "<!DOCTYPE html>" +
                        "<html><head><meta charset='UTF-8'>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<title>登录成功</title>" +
                        "<style>" +
                        "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; " +
                        "display: flex; justify-content: center; align-items: center; height: 100vh; " +
                        "margin: 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }" +
                        ".container { text-align: center; background: white; padding: 40px; " +
                        "border-radius: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); }" +
                        ".success-icon { font-size: 60px; color: #52c41a; margin-bottom: 20px; }" +
                        "h1 { color: #333; margin: 0 0 10px 0; }" +
                        "p { color: #666; margin: 0; }" +
                        "</style></head><body>" +
                        "<div class='container'>" +
                        "<div class='success-icon'>✓</div>" +
                        "<h1>登录成功！</h1>" +
                        "<p>请返回电脑浏览器继续操作</p>" +
                        "</div></body></html>";
                    
                    return ResponseEntity.ok()
                            .header("Content-Type", "text/html; charset=UTF-8")
                            .body(html);
                }
            }
            
            // 常规登录流程
            String userJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(user);
            
            // 重定向到登录页并带上token（使用query参数传递）
            String redirectUrl = String.format(
                "https://superinventory.cfapps.us10-001.hana.ondemand.com/login?wechat_token=%s&wechat_user=%s",
                java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(userJson, java.nio.charset.StandardCharsets.UTF_8)
            );
            
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
        } catch (Exception e) {
            // 重定向到前端登录页并显示错误
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "https://superinventory.cfapps.us10-001.hana.ondemand.com/login?error=auth_failed&msg=" + 
                        java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8))
                    .build();
        }
    }
    
    /**
     * 验证Token是否有效
     * POST /api/auth/verify
     * Header: Authorization: Bearer <token>
     * 
     * @param authHeader Authorization header
     * @return Token验证结果
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Token不能为空");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            String token = authHeader.substring(7);
            boolean isValid = jwtTokenProvider.validateToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "valid", isValid,
                "expired", isValid ? jwtTokenProvider.isTokenExpired(token) : true
            ));
            response.put("message", isValid ? "Token有效" : "Token无效");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Token验证失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 获取当前用户信息
     * GET /api/auth/userinfo
     * Header: Authorization: Bearer <token>
     * 
     * @param authHeader Authorization header
     * @return 用户信息
     */
    @GetMapping("/userinfo")
    public ResponseEntity<?> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "未授权访问");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            String token = authHeader.substring(7);
            Map<String, Object> userInfo = weChatAuthService.getUserByToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of("user", userInfo));
            response.put("message", "获取用户信息成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取用户信息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
    
    /**
     * 退出登录
     * POST /api/auth/logout
     * 
     * @return 退出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // 由于使用JWT，服务端不需要维护session
        // 客户端只需删除本地存储的token即可
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "退出登录成功");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 微信服务器验证端点
     * GET /api/auth/wx_verify
     * 
     * 微信公众平台会发送GET请求到此URL进行验证
     * 
     * @param signature 微信加密签名
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param echostr 随机字符串
     * @return 原样返回echostr表示验证成功
     */
    @GetMapping("/wx_verify")
    public ResponseEntity<String> wechatVerify(
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {
        try {
            // 1. 将token、timestamp、nonce三个参数进行字典序排序
            String[] arr = new String[]{wechatToken, timestamp, nonce};
            Arrays.sort(arr);
            
            // 2. 将三个参数字符串拼接成一个字符串进行sha1加密
            StringBuilder content = new StringBuilder();
            for (String s : arr) {
                content.append(s);
            }
            
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(content.toString().getBytes());
            
            StringBuilder hexStr = new StringBuilder();
            for (byte b : digest) {
                String shaHex = Integer.toHexString(b & 0xFF);
                if (shaHex.length() < 2) {
                    hexStr.append(0);
                }
                hexStr.append(shaHex);
            }
            
            // 3. 将sha1加密后的字符串与signature对比
            if (hexStr.toString().equals(signature)) {
                // 验证成功，返回echostr
                return ResponseEntity.ok(echostr);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("验证失败");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("验证异常: " + e.getMessage());
        }
    }
    
    /**
     * 生成二维码登录数据
     * GET /api/auth/qrcode/generate
     * 
     * @return 二维码state和授权URL
     */
    @GetMapping("/qrcode/generate")
    public ResponseEntity<?> generateQRCode() {
        try {
            // 生成二维码state
            QRCodeState qrcodeState = qrcodeService.generateQRCodeState();
            
            // 生成微信授权URL
            String authUrl = weChatAuthService.generateAuthUrl(qrcodeState.getState());
            
            // 计算过期时间（秒）
            long expiresIn = Duration.between(
                qrcodeState.getCreateTime(),
                qrcodeState.getExpireTime()
            ).getSeconds();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "state", qrcodeState.getState(),
                "authUrl", authUrl,
                "expiresIn", expiresIn
            ));
            response.put("message", "生成二维码成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "生成二维码失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 检查二维码登录状态
     * GET /api/auth/qrcode/check?state=xxx
     * 
     * @param state 二维码唯一标识
     * @return 登录状态和Token（如果已登录）
     */
    @GetMapping("/qrcode/check")
    public ResponseEntity<?> checkQRCodeStatus(@RequestParam("state") String state) {
        try {
            QRCodeState qrcodeState = qrcodeService.getQRCodeState(state);
            
            if (qrcodeState == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", Map.of("status", "expired"));
                response.put("message", "二维码不存在或已过期");
                return ResponseEntity.ok(response);
            }
            
            String status = qrcodeState.getStatus();
            Map<String, Object> data = new HashMap<>();
            data.put("status", status);
            
            if ("success".equals(status)) {
                // 登录成功，返回token
                data.put("token", qrcodeState.getToken());
                
                // 使用后立即删除state（一次性使用）
                qrcodeService.removeQRCodeState(state);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("message", "success".equals(status) ? "登录成功" : "等待扫码");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "检查状态失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
