package com.yantai.superinventory.service;

import com.yantai.superinventory.model.QRCodeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 二维码登录服务
 * 管理二维码登录状态
 */
@Service
public class QRCodeService {
    
    private static final Logger logger = LoggerFactory.getLogger(QRCodeService.class);
    
    /**
     * 存储二维码状态的Map
     * Key: state (UUID)
     * Value: QRCodeState对象
     */
    private final Map<String, QRCodeState> qrcodeStateMap = new ConcurrentHashMap<>();
    
    /**
     * 生成新的二维码state
     * 
     * @return QRCodeState对象
     */
    public QRCodeState generateQRCodeState() {
        String state = UUID.randomUUID().toString();
        QRCodeState qrcodeState = new QRCodeState(state);
        
        qrcodeStateMap.put(state, qrcodeState);
        
        logger.info("生成新的二维码state: {}, 总数: {}", state, qrcodeStateMap.size());
        
        return qrcodeState;
    }
    
    /**
     * 获取二维码状态
     * 
     * @param state 唯一标识符
     * @return QRCodeState对象，如果不存在或已过期则返回null
     */
    public QRCodeState getQRCodeState(String state) {
        QRCodeState qrcodeState = qrcodeStateMap.get(state);
        
        if (qrcodeState == null) {
            logger.warn("二维码state不存在: {}", state);
            return null;
        }
        
        // 检查是否过期
        if (qrcodeState.isExpired()) {
            logger.info("二维码已过期: {}", state);
            qrcodeState.markExpired();
            // 可以选择立即删除过期的state
            // qrcodeStateMap.remove(state);
        }
        
        return qrcodeState;
    }
    
    /**
     * 更新二维码状态为成功
     * 
     * @param state 唯一标识符
     * @param token JWT Token
     * @return 是否更新成功
     */
    public boolean markQRCodeSuccess(String state, String token) {
        QRCodeState qrcodeState = qrcodeStateMap.get(state);
        
        if (qrcodeState == null) {
            logger.warn("尝试更新不存在的二维码state: {}", state);
            return false;
        }
        
        if (qrcodeState.isExpired()) {
            logger.warn("尝试更新已过期的二维码state: {}", state);
            return false;
        }
        
        qrcodeState.markSuccess(token);
        logger.info("二维码登录成功: {}", state);
        
        return true;
    }
    
    /**
     * 删除二维码状态（登录成功后使用）
     * 
     * @param state 唯一标识符
     */
    public void removeQRCodeState(String state) {
        QRCodeState removed = qrcodeStateMap.remove(state);
        if (removed != null) {
            logger.info("删除二维码state: {}, 剩余: {}", state, qrcodeStateMap.size());
        }
    }
    
    /**
     * 定时清理过期的二维码状态
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 60秒 = 1分钟
    public void cleanExpiredQRCodeStates() {
        int beforeSize = qrcodeStateMap.size();
        int cleanedCount = 0;
        
        // 遍历所有state，删除过期的
        qrcodeStateMap.entrySet().removeIf(entry -> {
            QRCodeState state = entry.getValue();
            boolean expired = state.isExpired();
            if (expired) {
                logger.debug("清理过期的二维码state: {}", entry.getKey());
            }
            return expired;
        });
        
        cleanedCount = beforeSize - qrcodeStateMap.size();
        
        if (cleanedCount > 0) {
            logger.info("定时清理完成: 清理了 {} 个过期state, 剩余 {} 个", 
                       cleanedCount, qrcodeStateMap.size());
        }
    }
    
    /**
     * 获取当前二维码状态数量（用于监控）
     * 
     * @return 当前state数量
     */
    public int getQRCodeStateCount() {
        return qrcodeStateMap.size();
    }
    
    /**
     * 清空所有二维码状态（用于测试或重启）
     */
    public void clearAllQRCodeStates() {
        int size = qrcodeStateMap.size();
        qrcodeStateMap.clear();
        logger.warn("清空所有二维码state, 共清理: {}", size);
    }
}
