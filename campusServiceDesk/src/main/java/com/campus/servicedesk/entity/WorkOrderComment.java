package com.campus.servicedesk.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkOrderComment {
    private Integer id;
    private Integer workOrderId;
    private Integer userId;
    private Integer rating;
    private String content;
    private LocalDateTime createTime;
}