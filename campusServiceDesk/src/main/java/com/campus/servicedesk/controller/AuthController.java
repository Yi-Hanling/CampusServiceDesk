package com.campus.servicedesk.controller;

import com.campus.servicedesk.common.Result;
import com.campus.servicedesk.entity.SysUser;
import com.campus.servicedesk.mapper.SysUserMapper;
import com.campus.servicedesk.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JwtUtil jwtUtil;

    // 初始化密码加密器
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 1. 注册接口
    @PostMapping("/register")
    public Result<String> register(@RequestBody SysUser user) {
        if (sysUserMapper.selectByUsername(user.getUsername()) != null) {
            return Result.failure(400, "用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 核心修复：公开注册接口，绝不能信任前端传的 role，强制设定为 0（学生）
        user.setRole(0);

        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        sysUserMapper.insertUser(user);
        return Result.success("注册成功", null);
    }

    // 2. 登录接口
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody SysUser loginUser) {
        // 1. 根据用户名查库
        SysUser user = sysUserMapper.selectByUsername(loginUser.getUsername());

        // 2. 校验用户是否存在 以及 密码是否匹配 (使用 BCrypt 的 matches 方法)
        if (user == null || !passwordEncoder.matches(loginUser.getPassword(), user.getPassword())) {
            return Result.failure(400, "用户名或密码错误");
        }

        // 3. 校验账号状态
        if (user.getStatus() == 0) {
            return Result.failure(403, "账号已被禁用，请联系管理员");
        }

        // 4. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 5. 封装返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", user.getRole());
        data.put("realName", user.getRealName());

        return Result.success("登录成功", data);
    }

    // 管理员启用/禁用账号
    @PutMapping("/user/{id}/status")
    public Result<String> changeUserStatus(@PathVariable Integer id, @RequestParam Integer status, HttpServletRequest request) {
        if ((Integer) request.getAttribute("role") != 2) return Result.failure(403, "无权限");
        sysUserMapper.updateUserStatus(id, status);
        return Result.success("账号状态已更新", null);
    }

    // 1. 获取所有维修工（给分配弹窗的下拉框用）
    @GetMapping("/users/workers")
    public Result<List<SysUser>> getAllWorkers() {
        // 你需要在 SysUserMapper 里加一个：@Select("SELECT id, real_name FROM sys_user WHERE role = 1 AND status = 1") List<SysUser> selectAllWorkers();
        return Result.success(sysUserMapper.selectAllWorkers());
    }

    // 2. 获取所有用户（给用户管理表格用）
    @GetMapping("/users/list")
    public Result<List<SysUser>> getAllUsers(HttpServletRequest request) {
        if ((Integer) request.getAttribute("role") != 2) return Result.failure(403, "无权限");
        // 你需要在 SysUserMapper 里加一个：@Select("SELECT * FROM sys_user") List<SysUser> selectAllUsers();
        return Result.success(sysUserMapper.selectAllUsers());
    }
}