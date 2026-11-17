package com.campus.servicedesk.exception;

import com.campus.servicedesk.common.Result;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获所有异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        e.printStackTrace();
        String message = e.getMessage();

        if (message != null) {
            // 1. 拦截分配失败的异常
            if (message.contains("分配失败")) {
                return Result.failure(400, "分配失败：该账号不是合法维修工或已被禁用");
            }
            // 2. 拦截状态流转的异常
            if (message.contains("非法的状态流转")) {
                return Result.failure(400, "非法操作：工单状态不正确或已完成");
            }
            // 3. 新增：拦截评价失败的异常
            if (message.contains("评价失败")) {
                return Result.failure(400, message.split("\r\n")[0]); // 只取第一行报错
            }
            // 4. 新增：拦截确认失败的异常（越权拦截）
            if (message.contains("确认失败")) {
                return Result.failure(400, "确认失败：工单不存在、状态不对或您无权操作该工单");
            }
            if (message.contains("请勿重复提交")) {
                return Result.failure(400, "请勿重复提交：操作太频繁，请稍后再试");
            }
            if (message.contains("评价失败")) {
                return Result.failure(400, message); // 直接把后端真实报错返回给前端
            }
        }

        return Result.failure(500, "系统异常，请联系管理员");
    }
    // 捕获参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        return Result.failure(400, msg);
    }

}