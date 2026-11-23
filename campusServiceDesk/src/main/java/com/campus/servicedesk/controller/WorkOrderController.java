package com.campus.servicedesk.controller;

import com.campus.servicedesk.common.PageResult;
import com.campus.servicedesk.common.Result;
import com.campus.servicedesk.dto.WorkOrderCreateRequest;
import com.campus.servicedesk.dto.WorkOrderPageQuery;
import com.campus.servicedesk.entity.WorkOrder;
import com.campus.servicedesk.entity.WorkOrderLog;
import com.campus.servicedesk.mapper.WorkOrderLogMapper;
import com.campus.servicedesk.mapper.WorkOrderMapper;
import com.campus.servicedesk.service.WorkOrderService;
import com.campus.servicedesk.vo.CommentDetailVO;
import com.campus.servicedesk.vo.OrderStatisticsVO;
import com.campus.servicedesk.vo.WorkOrderDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class WorkOrderController {

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private WorkOrderLogMapper workOrderLogMapper;

    // 测试接口 1：获取所有工单
    @GetMapping("/list")
    public Result<List<WorkOrder>> listAllOrders() {
        List<WorkOrder> orders = workOrderMapper.selectAll();
        return Result.success(orders);
    }

    // 测试接口 2：查看某个工单的日志（验证触发器）
    @GetMapping("/{id}/logs")
    public Result<List<WorkOrderLog>> getOrderLogs(@PathVariable Integer id) {
        List<WorkOrderLog> logs = workOrderLogMapper.selectByOrderId(id);
        return Result.success(logs);
    }

    @Autowired
    private WorkOrderService workOrderService;

    // 新增：学生提交工单接口
    // 改造后的提交工单接口
    @PostMapping("/create")
    public Result<WorkOrder> createOrder(@Validated @RequestBody WorkOrderCreateRequest request, HttpServletRequest httpRequest) {
        // 从拦截器解析出的 Token 中获取当前登录用户的 ID
        Integer realCreatorId = (Integer) httpRequest.getAttribute("userId");

        System.out.println("==== 当前获取到的用户ID: " + realCreatorId + " ====");

        // 将真实 ID 塞入请求
        request.setCreatorId(realCreatorId);

        return Result.success("工单提交成功", workOrderService.createOrder(request));
    }
    // 维修工接单接口
    @PutMapping("/{id}/accept")
    public Result<String> acceptOrder(@PathVariable Integer id, @RequestParam Integer workerId) {
        workOrderService.acceptOrder(id, workerId);
        return Result.success("接单成功", null);
    }

    // 管理员分配工单接口
    @PutMapping("/{id}/assign")
    public Result<String> assignOrder(@PathVariable Integer id,
                                      @RequestParam Integer workerId,
                                      HttpServletRequest httpRequest) {
        // 获取当前登录人的角色（由拦截器塞入）
        Integer role = (Integer) httpRequest.getAttribute("role");

        workOrderService.assignOrder(id, workerId, role);
        return Result.success("分配成功", null);
    }

    @PutMapping("/{id}/complete")
    public Result<String> completeOrder(@PathVariable Integer id, HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        Integer role = (Integer) httpRequest.getAttribute("role");
        workOrderService.completeOrder(id, userId, role);
        return Result.success("维修完工，等待学生确认", null);
    }

    @PostMapping("/{id}/comment")
    public Result<String> addComment(@PathVariable Integer id,
                                     @RequestParam Integer rating,
                                     @RequestParam String content,
                                     HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        workOrderService.addComment(id, userId, rating, content);
        return Result.success("评价成功", null);
    }

    // 学生查看我的提交
    @GetMapping("/my")
    public Result<List<WorkOrder>> getMyOrders(HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return Result.success(workOrderMapper.selectByCreatorId(userId));
    }

    // 维修工查看分配给我的
    @GetMapping("/assigned")
    public Result<List<WorkOrder>> getAssignedOrders(HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return Result.success(workOrderMapper.selectByAssigneeId(userId));
    }

    // 学生确认完工
    @PutMapping("/{id}/confirm")
    public Result<String> confirmOrder(@PathVariable Integer id, HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        workOrderService.confirmOrder(id, userId);
        return Result.success("确认成功，工单已完结", null);
    }

    // 分页查询工单
    @GetMapping("/page")
    public Result<PageResult<WorkOrder>> getPageOrders(WorkOrderPageQuery query) {
        // 1. 查总数
        long total = workOrderMapper.countByCondition(query.getStatus(), query.getKeyword());
        // 2. 查当前页数据
        List<WorkOrder> records = workOrderMapper.selectPage(query.getStatus(), query.getKeyword(), query.getOffset(), query.getPageSize());

        return Result.success(new PageResult<>(total, records));
    }

    // 管理员数据统计接口
    @GetMapping("/statistics")
    public Result<List<OrderStatisticsVO>> getStatistics(HttpServletRequest httpRequest) {
        // 安全校验：只有管理员才能看统计
        Integer role = (Integer) httpRequest.getAttribute("role");
        if (role != 2) {
            return Result.failure(403, "权限不足：只有管理员可查看统计");
        }
        return Result.success(workOrderMapper.getStatistics());
    }

    // 获取工单详情（所有登录角色都能看）
    @GetMapping("/{id}/detail")
    public Result<WorkOrderDetailVO> getDetail(@PathVariable Integer id) {
        WorkOrderDetailVO detail = workOrderMapper.getDetailById(id);
        if (detail == null) {
            return Result.failure(404, "工单不存在");
        }
        return Result.success(detail);
    }

    // 管理员驳回工单
    @PutMapping("/{id}/reject")
    public Result<String> rejectOrder(@PathVariable Integer id, HttpServletRequest httpRequest) {
        if ((Integer) httpRequest.getAttribute("role") != 2) return Result.failure(403, "权限不足");
        if (workOrderMapper.rejectOrder(id) == 0) throw new RuntimeException("驳回失败：工单不存在或状态不符");
        return Result.success("驳回成功", null);
    }

    // 维修工拒绝接单
    @PutMapping("/{id}/refuse")
    public Result<String> refuseOrder(@PathVariable Integer id, HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        if ((Integer) httpRequest.getAttribute("role") != 1) return Result.failure(403, "权限不足");
        if (workOrderMapper.refuseOrder(id, userId) == 0) throw new RuntimeException("拒绝失败：工单状态已变更或无权操作");
        return Result.success("已拒绝接单，工单退回池中", null);

    }

    @PutMapping("/{id}/reopen")
    public Result<String> reopenOrder(@PathVariable Integer id, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        int rows = workOrderMapper.reopenOrder(id, userId);
        if (rows == 0) throw new RuntimeException("返工失败：工单状态不对或无权操作");
        return Result.success("已申请重新处理，已退回维修工", null);
    }

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 学生/维修工上传图片
    @PostMapping("/{id}/uploadImage")
    public Result<String> uploadImage(@PathVariable Integer id,
                                      @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.failure(400, "上传失败：文件不能为空");
        }

        // 1. 校验文件大小（限制 5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.failure(400, "上传失败：图片大小不能超过 5MB");
        }

        try {
            // 2. 生成唯一文件名，防止重名覆盖
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + suffix;

            // 👇 3. 核心修复：使用 File 对象，它会自动处理路径斜杠问题
            File destDir = new File(uploadDir);
            if (!destDir.exists()) {
                destDir.mkdirs(); // 如果没有目录则自动创建
            }

            // 把父目录和文件名分开传，这是最安全的做法
            File destFile = new File(destDir, newFileName);

            // 打印一下真实保存的路径，方便你在 IDEA 控制台排错
            System.out.println("图片真实保存路径: " + destFile.getAbsolutePath());

            file.transferTo(destFile);

            // 4. 将相对路径存入数据库（数据库中存的是 /uploads/xxx.png）
            String imageUrl = "/uploads/" + newFileName;
            workOrderMapper.insertImage(id, imageUrl);

            return Result.success("上传成功", imageUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.failure(500, "上传失败：服务器内部错误");
        }
    }

    // 维修工查看待抢单池
    @GetMapping("/pending")
    public Result<List<WorkOrder>> getPendingOrders() {
        return Result.success(workOrderMapper.selectPendingOrders());
    }

    // 维修工抢单
    @PutMapping("/{id}/grab")
    public Result<String> grabOrder(@PathVariable Integer id, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        int rows = workOrderMapper.grabOrder(id, userId);
        if (rows == 0) throw new RuntimeException("抢单失败：手慢了，工单已被别人抢走");
        return Result.success("抢单成功", null);
    }

    // 统一获取评价列表接口
    @GetMapping("/comments")
    public Result<List<CommentDetailVO>> getComments(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        Integer userId = (Integer) request.getAttribute("userId");

        if (role == null) return Result.failure(401, "未登录");

        if (role == 2) {
            // 管理员查所有
            return Result.success(workOrderMapper.selectAllComments());
        } else if (role == 1) {
            // 维修工查自己的
            return Result.success(workOrderMapper.selectCommentsByWorkerId(userId));
        } else {
            // 学生查自己的
            return Result.success(workOrderMapper.selectCommentsByStudentId(userId));
        }
    }
    // 查询工单的图片列表
    @GetMapping("/{id}/images")
    public Result<List<String>> getOrderImages(@PathVariable Integer id) {
        return Result.success(workOrderMapper.selectImagesByOrderId(id));
    }
}