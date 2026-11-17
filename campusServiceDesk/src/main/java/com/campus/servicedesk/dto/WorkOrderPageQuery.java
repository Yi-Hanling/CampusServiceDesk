package com.campus.servicedesk.dto;

import lombok.Data;

@Data
public class WorkOrderPageQuery {
    private Integer pageNum = 1; // 当前页，默认第1页
    private Integer pageSize = 10; // 每页条数，默认10
    private Integer status; // 按状态筛选（可为空）
    private String keyword; // 按标题模糊搜索（可为空）

    // 计算 SQL Server 分页的偏移量
    public Integer getOffset() {
        return (pageNum - 1) * pageSize;
    }
}