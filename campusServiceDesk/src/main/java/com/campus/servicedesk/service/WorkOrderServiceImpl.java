package com.campus.servicedesk.service;

import com.campus.servicedesk.dto.WorkOrderCreateRequest;
import com.campus.servicedesk.entity.WorkOrder;
import com.campus.servicedesk.mapper.WorkOrderMapper;
import com.campus.servicedesk.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service // 标记为 Service 组件
public class WorkOrderServiceImpl implements WorkOrderService {

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Override
    public WorkOrder createOrder(WorkOrderCreateRequest request) {
        // 👇 新增：幂等性防重复提交校验
        int count = workOrderMapper.countRecentSimilarOrder(request.getCreatorId(), request.getTitle());
        if (count > 0) {
            throw new RuntimeException("请勿重复提交：您已在1分钟内提交过相同标题的工单，请稍后再试");
        }
        // 1. 将 DTO 转换为 Entity
        WorkOrder order = new WorkOrder();
        order.setTitle(request.getTitle());
        order.setContent(request.getContent());
        order.setCreatorId(request.getCreatorId());
        order.setStatus(0); // 0: 待处理，新提交的工单默认是待处理

        // 2. 插入数据库
        // 💡 注意：此时数据库触发器 TR_WorkOrder_Insert 会自动触发，写入一条日志！
        workOrderMapper.insertOrder(order);

        return order;
    }

    @Override
    public void acceptOrder(Integer orderId, Integer workerId) {
        // 在 Service 层调用 Mapper 层去干活
        workOrderMapper.acceptOrder(orderId, workerId);
    }

    // 实现类
    @Override
    public void assignOrder(Integer orderId, Integer workerId, Integer adminRole) {
        if (adminRole != 2) {
            throw new RuntimeException("权限不足：只有管理员才能分配工单");
        }
        int rows = workOrderMapper.assignOrder(orderId, workerId);
        if (rows == 0) {
            throw new RuntimeException("分配失败：工单不存在或已被分配");
        }
    }

    // 注意：加上 @Transactional，如果这里报错，数据库操作会回滚
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Integer orderId, Integer workerId, Integer role) {
        if (role != 1) {
            throw new RuntimeException("权限不足：只有维修工才能操作完工");
        }
        int rows = workOrderMapper.completeOrder(orderId, workerId);
        if (rows == 0) {
            throw new RuntimeException("完工失败：工单不存在、状态不对或您无权操作");
        }
    }

    // 实现类
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addComment(Integer orderId, Integer userId, Integer rating, String content) {
        // 👇 新增：主动查库校验状态，不要全指望触发器
        WorkOrder order = workOrderMapper.selectById(orderId);

        if (order == null) {
            throw new RuntimeException("评价失败：工单不存在");
        }
        // 如果状态不是 3（已完成），直接拒绝
        if (order.getStatus() != 3) {
            throw new RuntimeException("评价失败：该工单尚未最终确认完成，请刷新页面查看最新状态");
        }

        // 校验通过，才去插入评价（此时触发器也会再兜底一次）
        workOrderMapper.insertComment(orderId, userId, rating, content);
    }

    // WorkOrderServiceImpl 实现类中添加：
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Integer orderId, Integer userId) {
        // 直接更新，如果因为状态不对或不是本人提交导致行数为0，则报错
        int rows = workOrderMapper.confirmOrder(orderId, userId);
        if (rows == 0) {
            throw new RuntimeException("确认失败：工单不存在、状态不是待确认或您无权操作");
        }
    }


}