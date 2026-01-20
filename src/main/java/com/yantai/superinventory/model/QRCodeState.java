package com.yantai.superinventory.model;

import java.time.LocalDateTime;

/**
 * 二维码登录状态实体类
 * 用于存储扫码登录的状态信息
 */
public class QRCodeState {
    
    /**
     * 唯一标识符（UUID）
     */
    private String state;
    
    /**
     * 登录状态：pending（等待扫码）、success（登录成功）、expired（已过期）
     */
    private String status;
    
    /**
     * JWT Token（登录成功后存储）
     */
    private String token;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 过期时间（创建时间 + 5分钟）
     */
    private LocalDateTime expireTime;
    
    /**
     * 构造函数
     */
    public QRCodeState() {
    }
    
    /**
     * 构造函数
     * 
     * @param state 唯一标识符
     */
    public QRCodeState(String state) {
        this.state = state;
        this.status = "pending";
        this.createTime = LocalDateTime.now();
        this.expireTime = this.createTime.plusMinutes(5);
    }
    
    /**
     * 检查是否已过期
     * 
     * @return true表示已过期，false表示未过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expireTime);
    }
    
    /**
     * 标记为成功并保存token
     * 
     * @param token JWT Token
     */
    public void markSuccess(String token) {
        this.status = "success";
        this.token = token;
    }
    
    /**
     * 标记为过期
     */
    public void markExpired() {
        this.status = "expired";
    }
    
    // Getters and Setters
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getExpireTime() {
        return expireTime;
    }
    
    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
    
    @Override
    public String toString() {
        return "QRCodeState{" +
                "state='" + state + '\'' +
                ", status='" + status + '\'' +
                ", token='" + (token != null ? "***" : "null") + '\'' +
                ", createTime=" + createTime +
                ", expireTime=" + expireTime +
                '}';
    }
}
