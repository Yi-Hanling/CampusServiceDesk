package com.campus.servicedesk.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDetailVO {
    private Integer id;
    private Integer workOrderId;
    private String orderTitle;    // 工单标题
    private String studentName;   // 学生姓名
    private String workerName;    // 维修工姓名
    private Integer rating;       // 评分
    private String content;       // 评价内容
    private LocalDateTime createTime;
}