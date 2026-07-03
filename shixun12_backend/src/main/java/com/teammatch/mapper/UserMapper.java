package com.teammatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teammatch.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户数据访问层
 * 用于查询用户基本信息
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // 继承 BaseMapper 提供的基础 CRUD 方法

    /**
     * 批量查询用户基本信息
     * 用于 M5-1B 待评价成员列表中获取用户昵称和头像
     *
     * @param userIds 用户 ID 列表
     * @return 用户列表
     */
    @Select("<script>" +
            "SELECT id, nickname, avatar_url FROM user WHERE id IN " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<User> findByIds(@Param("userIds") List<Long> userIds);

    @Update("UPDATE user SET credit_score = credit_score + #{delta} WHERE id = #{userId}")
    int updateCreditScore(@Param("userId") Long userId, @Param("delta") int delta);
}
