package com.campus.servicedesk.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkOrderImage {
    private Integer id;
    private Integer workOrderId;
    private String imageUrl;
    private LocalDateTime uploadTime;
}