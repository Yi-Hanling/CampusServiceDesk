package com.campus.servicedesk.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkOrderLog {
    private Integer id;
    private Integer workOrderId;
    private Integer operatorId;
    private String actionType;
    private Integer oldStatus;
    private Integer newStatus;
    private String remark;
    private LocalDateTime createTime;
}