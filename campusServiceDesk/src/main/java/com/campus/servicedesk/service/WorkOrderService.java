package com.campus.servicedesk.service;

import com.campus.servicedesk.dto.WorkOrderCreateRequest;
import com.campus.servicedesk.entity.WorkOrder;
import org.springframework.transaction.annotation.Transactional;

public interface WorkOrderService {
    // 学生提交工单
    WorkOrder createOrder(WorkOrderCreateRequest request);

    // 维修工接单（这里只定义方法，具体怎么调用数据库，去 Impl 里面写）
    void acceptOrder(Integer orderId, Integer workerId);

    void assignOrder(Integer orderId, Integer workerId, Integer adminRole);

    void completeOrder(Integer orderId, Integer workerId, Integer role);

    // 接口
    void addComment(Integer orderId, Integer userId, Integer rating, String content);

    void confirmOrder(Integer orderId, Integer userId);
}