package com.yantai.superinventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理应用程序中的异常，返回友好的错误信息
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理所有未捕获的异常
     * 
     * @param ex 异常对象
     * @param request 请求对象
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex, WebRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "服务器内部错误: " + ex.getMessage());
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        
        // 记录日志
        System.err.println("全局异常: " + ex.getMessage());
        ex.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * 处理运行时异常
     * 
     * @param ex 异常对象
     * @param request 请求对象
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex, WebRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        
        // 记录日志
        System.err.println("运行时异常: " + ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 处理非法参数异常
     * 
     * @param ex 异常对象
     * @param request 请求对象
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "参数错误: " + ex.getMessage());
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 处理空指针异常
     * 
     * @param ex 异常对象
     * @param request 请求对象
     * @return 错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<?> handleNullPointerException(NullPointerException ex, WebRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "数据不存在或为空");
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        
        // 记录日志
        System.err.println("空指针异常: " + ex.getMessage());
        ex.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}
