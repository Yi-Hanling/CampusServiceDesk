package com.campus.servicedesk.common;

import lombok.Getter;

@Getter
public enum WorkOrderStatus {
    PENDING_ASSIGNMENT(0, "待分配"),
    IN_PROGRESS(1, "处理中"),
    PENDING_CONFIRMATION(2, "待学生确认"),
    COMPLETED(3, "已完成"),
    REJECTED(4, "已驳回");

    private final int code;
    private final String description;

    WorkOrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}