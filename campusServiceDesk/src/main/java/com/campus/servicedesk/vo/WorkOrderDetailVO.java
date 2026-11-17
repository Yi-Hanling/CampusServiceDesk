package com.campus.servicedesk.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkOrderDetailVO {
    private Integer id;
    private String title;
    private String content;
    private Integer status;
    private String creatorName; // 提交人姓名
    private String assigneeName; // 维修工姓名
    private LocalDateTime createTime;
}