package com.campus.servicedesk.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data // Lombok 注解，自动生成 Getter/Setter/toString
public class SysUser {
    private Integer id;
    private String username;
    private String password;
    private String realName;
    private Integer role;      // 0:学生, 1:维修工, 2:管理员
    private Integer status;    // 1:正常, 0:禁用
    private LocalDateTime createTime;
}
