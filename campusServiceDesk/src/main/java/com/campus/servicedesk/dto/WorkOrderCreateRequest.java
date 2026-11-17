package com.campus.servicedesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkOrderCreateRequest {
    @NotBlank(message = "工单标题不能为空")
    @Size(max = 50, message = "标题长度不能超过50")
    private String title;

    private String content;

    private Integer creatorId;
}