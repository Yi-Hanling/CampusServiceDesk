package com.campus.servicedesk.vo;

import lombok.Data;

@Data
public class OrderStatisticsVO {
    private Integer status; // 状态代码
    private Integer orderCount; // 该状态的工单数量
}