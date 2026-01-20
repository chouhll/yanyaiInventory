package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.User;
import com.yantai.superinventory.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 获取所有用户列表
     * GET /api/users
     * 
     * @return 用户列表
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            
            // 转换为简化的响应格式
            List<Map<String, Object>> userList = new ArrayList<>();
            for (User user : users) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("wechatOpenid", user.getWechatOpenid());
                userMap.put("nickname", user.getNickname());
                userMap.put("role", user.getRole());
                userMap.put("createdAt", user.getCreatedAt());
                userMap.put("lastLoginAt", user.getLastLoginAt());
                userMap.put("isActive", user.getIsActive());
                userList.add(userMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userList);
            response.put("total", users.size());
            response.put("message", "获取用户列表成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取用户列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 统计用户数量
     * GET /api/users/stats
     * 
     * @return 用户统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats() {
        try {
            long totalUsers = userRepository.count();
            long adminUsers = userRepository.countByRole("ADMIN");
            long normalUsers = userRepository.countByRole("USER");
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", totalUsers);
            stats.put("admin", adminUsers);
            stats.put("user", normalUsers);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("message", "获取用户统计成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取用户统计失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
