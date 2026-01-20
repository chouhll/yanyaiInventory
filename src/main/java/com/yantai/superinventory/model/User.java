package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户实体类
 * 存储通过微信OAuth认证的用户信息
 */
@Entity
@Table(name = "users")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    /**
     * 微信OpenID - 用户在当前公众号/小程序下的唯一标识
     */
    @Column(name = "wechat_openid", unique = true, nullable = false, length = 100)
    private String wechatOpenid;
    
    /**
     * 微信UnionID - 用户在开放平台下的唯一标识（可选）
     */
    @Column(name = "wechat_unionid", length = 100)
    private String wechatUnionid;
    
    /**
     * 用户昵称
     */
    @Column(name = "nickname", length = 100)
    private String nickname;
    
    /**
     * 用户头像URL
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    
    /**
     * 用户角色：USER-普通用户, ADMIN-管理员
     */
    @Column(name = "role", length = 50)
    private String role = "USER";
    
    /**
     * 账号创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 最后登录时间
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    /**
     * 账号是否激活
     */
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
