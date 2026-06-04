package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.BO.DeptAddBO;
import com.kakarote.ai_crm.entity.BO.DeptUpdateBO;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.VO.DeptVO;

import java.util.List;

public interface IManagerDeptService extends IService<ManagerDept> {

    /**
     * 查询部门树
     *
     * @return 树形结构的部门列表
     */
    List<DeptVO> queryDeptTree();

    /**
     * 添加部门
     *
     * @param bo 部门信息
     */
    void addDept(DeptAddBO bo);

    /**
     * 修改部门
     *
     * @param bo 部门信息
     */
    void updateDept(DeptUpdateBO bo);

    /**
     * 删除部门
     *
     * @param deptId 部门ID
     */
    void deleteDept(Long deptId);
}
