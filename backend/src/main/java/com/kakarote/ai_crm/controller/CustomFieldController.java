package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.CustomFieldAddBO;
import com.kakarote.ai_crm.entity.BO.CustomFieldUpdateBO;
import com.kakarote.ai_crm.entity.BO.FieldSortBO;
import com.kakarote.ai_crm.entity.VO.CustomFieldVO;
import com.kakarote.ai_crm.service.ICustomFieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/custom-field")
@Tag(name = "Custom Field APIs")
public class CustomFieldController {

    @Autowired
    private ICustomFieldService customFieldService;

    @PostMapping("/add")
    @Operation(summary = "Add custom field")
    @RequirePermission("customField:create")
    public Result<Long> add(@Valid @RequestBody CustomFieldAddBO bo) {
        return Result.ok(customFieldService.addField(bo));
    }

    @PostMapping("/update")
    @Operation(summary = "Update custom field")
    @RequirePermission("customField:edit")
    public Result<String> update(@Valid @RequestBody CustomFieldUpdateBO bo) {
        customFieldService.updateField(bo);
        return Result.ok();
    }

    @PostMapping("/disable/{id}")
    @Operation(summary = "Disable custom field")
    @RequirePermission("customField:edit")
    public Result<String> disable(
            @Parameter(description = "Field ID") @PathVariable("id") Long id) {
        customFieldService.disableField(id);
        return Result.ok();
    }

    @PostMapping("/enable/{id}")
    @Operation(summary = "Enable custom field")
    @RequirePermission("customField:edit")
    public Result<String> enable(
            @Parameter(description = "Field ID") @PathVariable("id") Long id) {
        customFieldService.enableField(id);
        return Result.ok();
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "Delete custom field")
    @RequirePermission("customField:delete")
    public Result<String> delete(
            @Parameter(description = "Field ID") @PathVariable("id") Long id) {
        customFieldService.deleteField(id);
        return Result.ok();
    }

    @GetMapping("/list/{entityType}")
    @Operation(summary = "List fields by entity")
    @RequirePermission("customField")
    public Result<List<CustomFieldVO>> list(
            @Parameter(description = "Entity type, such as customer or contact") @PathVariable("entityType") String entityType) {
        return Result.ok(customFieldService.getFieldsByEntity(entityType));
    }

    @GetMapping("/enabled/{entityType}")
    @Operation(summary = "List enabled fields by entity")
    public Result<List<CustomFieldVO>> enabled(
            @Parameter(description = "Entity type, such as customer or contact") @PathVariable("entityType") String entityType) {
        return Result.ok(customFieldService.getEnabledFieldsByEntity(entityType));
    }

    @PostMapping("/sort")
    @Operation(summary = "Sort custom fields")
    @RequirePermission("customField:edit")
    public Result<String> sort(@RequestBody List<FieldSortBO> sortList) {
        customFieldService.updateSortOrder(sortList);
        return Result.ok();
    }
}
