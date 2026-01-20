package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * 根据微信OpenID查找用户
     * @param wechatOpenid 微信OpenID
     * @return 用户对象（如果存在）
     */
    Optional<User> findByWechatOpenid(String wechatOpenid);
    
    /**
     * 根据微信UnionID查找用户
     * @param wechatUnionid 微信UnionID
     * @return 用户对象（如果存在）
     */
    Optional<User> findByWechatUnionid(String wechatUnionid);
    
    /**
     * 检查OpenID是否已存在
     * @param wechatOpenid 微信OpenID
     * @return 是否存在
     */
    boolean existsByWechatOpenid(String wechatOpenid);
    
    /**
     * 统计指定角色的用户数量
     * @param role 用户角色
     * @return 用户数量
     */
    long countByRole(String role);
}
