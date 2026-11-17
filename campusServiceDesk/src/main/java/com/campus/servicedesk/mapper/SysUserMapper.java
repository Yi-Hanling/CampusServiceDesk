package com.campus.servicedesk.mapper;

import com.campus.servicedesk.entity.SysUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysUserMapper {

    // 根据用户名查询用户（用于登录校验和注册重名校验）
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser selectByUsername(String username);

    // 插入新用户（用于注册）
    // 注意：如果使用了自增主键，可以加上 @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO sys_user (username, password, real_name, role, status) " +
            "VALUES (#{username}, #{password}, #{realName}, #{role}, 1)")
    int insertUser(SysUser user);

    // 管理员分页查看所有用户
    @Select("SELECT * FROM sys_user ORDER BY id DESC OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY")
    List<SysUser> selectUserPage(@Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    // 启用/禁用用户
    @Update("UPDATE sys_user SET status = #{status} WHERE id = #{id}")
    int updateUserStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    SysUser selectById(Integer id);

    // 查询所有正常的维修工（给分配弹窗的下拉框用）
    @Select("SELECT id, username, real_name, role FROM sys_user WHERE role = 1 AND status = 1")
    List<SysUser> selectAllWorkers();

    // 查询所有用户（给用户管理列表用）
    @Select("SELECT id, username, real_name, role, status, create_time FROM sys_user ORDER BY id DESC")
    List<SysUser> selectAllUsers();
}