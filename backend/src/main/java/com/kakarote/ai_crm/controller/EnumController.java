package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.FieldOption;
import com.kakarote.ai_crm.entity.VO.EnumVO;
import com.kakarote.ai_crm.service.ICustomFieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 枚举值控制器
 */
@RestController
@RequestMapping("/enum")
@Tag(name = "枚举值接口")
public class EnumController {

    private static final String DEFAULT_COLOR = "#6b7280";
    private static final Map<String, String> STAGE_COLORS = Map.of(
            "lead", "#6b7280",
            "qualified", "#3b82f6",
            "proposal", "#f59e0b",
            "negotiation", "#8b5cf6",
            "closed", "#22c55e",
            "lost", "#ef4444"
    );
    private static final Map<String, String> LEVEL_COLORS = Map.of(
            "A", "#22c55e",
            "B", "#3b82f6",
            "C", "#6b7280"
    );

    @Autowired
    private ICustomFieldService customFieldService;

    @GetMapping("/customerStage")
    @Operation(summary = "客户阶段枚举")
    public Result<List<EnumVO>> customerStage() {
        return Result.ok(toEnumList("customer", "stage", STAGE_COLORS));
    }

    @GetMapping("/customerLevel")
    @Operation(summary = "客户级别枚举")
    public Result<List<EnumVO>> customerLevel() {
        return Result.ok(toEnumList("customer", "level", LEVEL_COLORS));
    }

    @GetMapping("/relationType")
    @Operation(summary = "关系类型枚举")
    public Result<List<EnumVO>> relationType() {
        return Result.ok(toEnumList("relation", "relation_type", Map.of()));
    }

    @GetMapping("/relationSource")
    @Operation(summary = "关系来源枚举")
    public Result<List<EnumVO>> relationSource() {
        return Result.ok(toEnumList("relation", "source", Map.of()));
    }

    @GetMapping("/taskStatus")
    @Operation(summary = "任务状态枚举")
    public Result<List<EnumVO>> taskStatus() {
        return Result.ok(Arrays.asList(
            new EnumVO("pending", "待处理", "任务等待开始", "#f59e0b"),
            new EnumVO("in_progress", "进行中", "任务正在执行", "#3b82f6"),
            new EnumVO("completed", "已完成", "任务已完成", "#22c55e"),
            new EnumVO("cancelled", "已取消", "任务已取消", "#6b7280")
        ));
    }

    @GetMapping("/taskPriority")
    @Operation(summary = "任务优先级枚举")
    public Result<List<EnumVO>> taskPriority() {
        return Result.ok(Arrays.asList(
            new EnumVO("high", "高", "高优先级任务", "#ef4444"),
            new EnumVO("medium", "中", "中等优先级任务", "#f59e0b"),
            new EnumVO("low", "低", "低优先级任务", "#22c55e")
        ));
    }

    @GetMapping("/followUpType")
    @Operation(summary = "跟进类型枚举")
    public Result<List<EnumVO>> followUpType() {
        return Result.ok(Arrays.asList(
            new EnumVO("call", "电话", "电话沟通", "#3b82f6"),
            new EnumVO("meeting", "会议", "面对面或线上会议", "#8b5cf6"),
            new EnumVO("email", "邮件", "邮件沟通", "#22c55e"),
            new EnumVO("visit", "拜访", "上门拜访", "#f59e0b"),
            new EnumVO("other", "其他", "其他类型跟进", "#6b7280")
        ));
    }

    @GetMapping("/knowledgeType")
    @Operation(summary = "知识库类型枚举")
    public Result<List<EnumVO>> knowledgeType() {
        return Result.ok(Arrays.asList(
            new EnumVO("meeting", "会议记录", "会议纪要和记录", "#3b82f6"),
            new EnumVO("email", "邮件", "往来邮件记录", "#22c55e"),
            new EnumVO("recording", "录音", "通话或会议录音", "#8b5cf6"),
            new EnumVO("document", "文档", "一般文档资料", "#6b7280"),
            new EnumVO("proposal", "方案", "解决方案文档", "#f59e0b"),
            new EnumVO("contract", "合同", "合同文档", "#ef4444")
        ));
    }

    private List<EnumVO> toEnumList(String entityType, String fieldName, Map<String, String> colors) {
        return customFieldService.getFieldOptions(entityType, fieldName).stream()
                .filter(option -> option != null && option.getValue() != null)
                .map(option -> toEnum(option, colors))
                .toList();
    }

    private EnumVO toEnum(FieldOption option, Map<String, String> colors) {
        String value = option.getValue();
        return new EnumVO(
                value,
                option.getLabel() == null ? value : option.getLabel(),
                null,
                colors.getOrDefault(value, DEFAULT_COLOR)
        );
    }
}
