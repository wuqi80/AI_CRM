package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.DeptAddBO;
import com.kakarote.ai_crm.entity.BO.DeptUpdateBO;
import com.kakarote.ai_crm.entity.VO.DeptVO;
import com.kakarote.ai_crm.service.IManagerDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/managerDept")
@RestController
@Tag(name = "部门管理控制器")
public class ManagerDeptController {

    @Autowired
    private IManagerDeptService managerDeptService;

    @PostMapping("/queryDeptTree")
    @Operation(summary = "查询部门树")
    public Result<List<DeptVO>> queryDeptTree() {
        return Result.ok(managerDeptService.queryDeptTree());
    }

    @PostMapping("/add")
    @Operation(summary = "添加部门")
    public Result<String> add(@RequestBody DeptAddBO deptAddBO) {
        managerDeptService.addDept(deptAddBO);
        return Result.ok();
    }

    @PostMapping("/update")
    @Operation(summary = "修改部门")
    public Result<String> update(@RequestBody DeptUpdateBO deptUpdateBO) {
        managerDeptService.updateDept(deptUpdateBO);
        return Result.ok();
    }

    @PostMapping("/delete")
    @Operation(summary = "删除部门")
    public Result<String> delete(@Parameter(description = "部门ID") @RequestParam("deptId") Long deptId) {
        managerDeptService.deleteDept(deptId);
        return Result.ok();
    }
}
