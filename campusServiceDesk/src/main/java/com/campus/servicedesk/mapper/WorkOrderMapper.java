package com.campus.servicedesk.mapper;

import com.campus.servicedesk.entity.WorkOrder;
import com.campus.servicedesk.vo.CommentDetailVO;
import com.campus.servicedesk.vo.OrderStatisticsVO;
import com.campus.servicedesk.vo.WorkOrderDetailVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper // 标记为 MyBatis 的 Mapper
public interface WorkOrderMapper {

    // 查询所有工单
    @Select("SELECT * FROM work_order")
    List<WorkOrder> selectAll();

    // 插入一条工单（后面我们会用到）
    @Select("SELECT * FROM work_order WHERE id = #{id}")
    WorkOrder selectById(Integer id);

    // 插入工单，并返回自增主键（useGeneratedKeys = true）
    @Insert("INSERT INTO work_order (title, content, status, creator_id) VALUES (#{title}, #{content}, #{status}, #{creatorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(WorkOrder workOrder);

    // 更新工单状态
    @Update("UPDATE work_order SET status = #{status}, assignee_id = #{assigneeId}, update_time = GETDATE() WHERE id = #{id}")
    int updateOrderStatus(WorkOrder workOrder);

    @Update("UPDATE work_order SET status = 1, assignee_id = #{assigneeId}, update_time = GETDATE() WHERE id = #{id}")
    int acceptOrder(@Param("id") Integer id, @Param("assigneeId") Integer assigneeId);

    // 管理员分配工单
    @Update("UPDATE work_order SET status = 1, assignee_id = #{workerId}, update_time = GETDATE() WHERE id = #{orderId} AND status = 0")
    int assignOrder(@Param("orderId") Integer orderId, @Param("workerId") Integer workerId);

    // 维修工完工（把状态从 1 变成 2：待确认）
    @Update("UPDATE work_order SET status = 2, update_time = GETDATE() WHERE id = #{orderId} AND status = 1 AND assignee_id = #{workerId}")
    int completeOrder(@Param("orderId") Integer orderId, @Param("workerId") Integer workerId);

    // 学生确认完成（把状态从 2 变成 3：已完成）
    @Update("UPDATE work_order SET status = 3, update_time = GETDATE() WHERE id = #{orderId} AND status = 2 AND creator_id = #{creatorId}")
    int confirmOrder(@Param("orderId") Integer orderId, @Param("creatorId") Integer creatorId);
    // 插入评价
    @Insert("INSERT INTO work_order_comment (work_order_id, user_id, rating, content) VALUES (#{orderId}, #{userId}, #{rating}, #{content})")
    int insertComment(@Param("orderId") Integer orderId, @Param("userId") Integer userId, @Param("rating") Integer rating, @Param("content") String content);


    long countByCondition(@Param("status") Integer status, @Param("keyword") String keyword);


    List<WorkOrder> selectPage(@Param("status") Integer status,
                               @Param("keyword") String keyword,
                               @Param("offset") Integer offset,
                               @Param("pageSize") Integer pageSize);

    // 查询工单统计视图
    @Select("SELECT status, order_count AS orderCount FROM v_work_order_statistics")
    List<OrderStatisticsVO> getStatistics();

    // 查询工单详情（使用视图，展示复杂关联）
    @Select("SELECT * FROM v_work_order_detail WHERE id = #{id}")
    WorkOrderDetailVO getDetailById(Integer id);

    // 管理员驳回工单（只能是待分配状态才能驳回）
    @Update("UPDATE work_order SET status = 4, update_time = GETDATE() WHERE id = #{orderId} AND status = 0")
    int rejectOrder(@Param("orderId") Integer orderId);

    // 维修工拒绝接单（清空分配人，退回待分配状态）
    @Update("UPDATE work_order SET status = 0, assignee_id = NULL, update_time = GETDATE() WHERE id = #{orderId} AND status = 1 AND assignee_id = #{workerId}")
    int refuseOrder(@Param("orderId") Integer orderId, @Param("workerId") Integer workerId);

    // 管理员重新分配（把一个处理中的单子转给别人）
    @Update("UPDATE work_order SET assignee_id = #{newWorkerId}, update_time = GETDATE() WHERE id = #{orderId} AND status = 1")
    int reassignOrder(@Param("orderId") Integer orderId, @Param("newWorkerId") Integer newWorkerId);

    // 学生申请重新处理（状态从 2 打回 1）
    @Update("UPDATE work_order SET status = 1, update_time = GETDATE() WHERE id = #{orderId} AND status = 2 AND creator_id = #{creatorId}")
    int reopenOrder(@Param("orderId") Integer orderId, @Param("creatorId") Integer creatorId);

    // 查询该用户在过去1分钟内，是否提交过相同标题的工单
    @Select("SELECT COUNT(*) FROM work_order " +
            "WHERE creator_id = #{creatorId} AND title = #{title} " +
            "AND create_time >= DATEADD(minute, -1, GETDATE())")
    int countRecentSimilarOrder(@Param("creatorId") Integer creatorId, @Param("title") String title);

    @Insert("INSERT INTO work_order_image (work_order_id, image_url) VALUES (#{workOrderId}, #{imageUrl})")
    int insertImage(@Param("workOrderId") Integer workOrderId, @Param("imageUrl") String imageUrl);

    // 学生：只能看自己提交的
    @Select("SELECT * FROM work_order WHERE creator_id = #{userId} ORDER BY create_time DESC")
    List<WorkOrder> selectByCreatorId(@Param("userId") Integer userId);

    // 维修工：只能看分配给自己的
    @Select("SELECT * FROM work_order WHERE assignee_id = #{userId} ORDER BY create_time DESC")
    List<WorkOrder> selectByAssigneeId(@Param("userId") Integer userId);

    // 维修工查看所有待分配的工单
    @Select("SELECT * FROM work_order WHERE status = 0 ORDER BY create_time DESC")
    List<WorkOrder> selectPendingOrders();

    // 维修工抢单（注意条件是 status = 0，防止并发抢同一个）
    @Update("UPDATE work_order SET status = 1, assignee_id = #{workerId}, update_time = GETDATE() WHERE id = #{orderId} AND status = 0")
    int grabOrder(@Param("orderId") Integer orderId, @Param("workerId") Integer workerId);

    // 1. 管理员：查询所有评价
    @Select("SELECT c.id, c.work_order_id AS workOrderId, w.title AS orderTitle, " +
            "u.real_name AS studentName, a.real_name AS workerName, " +
            "c.rating, c.content, c.create_time AS createTime " +
            "FROM work_order_comment c " +
            "JOIN work_order w ON c.work_order_id = w.id " +
            "JOIN sys_user u ON c.user_id = u.id " +
            "LEFT JOIN sys_user a ON w.assignee_id = a.id " +
            "ORDER BY c.create_time DESC")
    List<CommentDetailVO> selectAllComments();

    // 2. 维修工：查询分配给自己的评价
    @Select("SELECT c.id, c.work_order_id AS workOrderId, w.title AS orderTitle, " +
            "u.real_name AS studentName, c.rating, c.content, c.create_time AS createTime " +
            "FROM work_order_comment c " +
            "JOIN work_order w ON c.work_order_id = w.id " +
            "JOIN sys_user u ON c.user_id = u.id " +
            "WHERE w.assignee_id = #{workerId} " +
            "ORDER BY c.create_time DESC")
    List<CommentDetailVO> selectCommentsByWorkerId(@Param("workerId") Integer workerId);

    // 3. 学生：查询自己提交的评价
    @Select("SELECT c.id, c.work_order_id AS workOrderId, w.title AS orderTitle, " +
            "a.real_name AS workerName, c.rating, c.content, c.create_time AS createTime " +
            "FROM work_order_comment c " +
            "JOIN work_order w ON c.work_order_id = w.id " +
            "LEFT JOIN sys_user a ON w.assignee_id = a.id " +
            "WHERE w.creator_id = #{studentId} " +
            "ORDER BY c.create_time DESC")
    List<CommentDetailVO> selectCommentsByStudentId(@Param("studentId") Integer studentId);

    // 根据工单ID查询图片列表
    @Select("SELECT image_url FROM work_order_image WHERE work_order_id = #{orderId}")
    List<String> selectImagesByOrderId(@Param("orderId") Integer orderId);
}