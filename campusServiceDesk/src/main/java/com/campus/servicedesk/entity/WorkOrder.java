package com.campus.servicedesk.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkOrder {
    private Integer id;
    private String title;
    private String content;
    private Integer status;      // 0:待处理, 1:处理中, 2:已完成, 3:已取消
    private Integer creatorId;
    private Integer assigneeId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}