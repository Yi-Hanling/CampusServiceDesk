package com.campus.servicedesk.mapper;

import com.campus.servicedesk.entity.WorkOrderLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WorkOrderLogMapper {
    // 根据工单ID查询日志
    @Select("SELECT * FROM work_order_log WHERE work_order_id = #{orderId}")
    List<WorkOrderLog> selectByOrderId(Integer orderId);
}