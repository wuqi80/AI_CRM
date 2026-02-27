package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.DeptAddBO;
import com.kakarote.ai_crm.entity.BO.DeptUpdateBO;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.VO.DeptVO;
import com.kakarote.ai_crm.mapper.ManagerDeptMapper;
import com.kakarote.ai_crm.service.IManagerDeptService;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.utils.RecursionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ManagerDeptServiceImpl extends ServiceImpl<ManagerDeptMapper, ManagerDept> implements IManagerDeptService {

    @Autowired
    private ManageUserService manageUserService;

    @Override
    public List<DeptVO> queryDeptTree() {
        List<DeptVO> allDepts = baseMapper.queryAllDeptWithUserCount();
        return RecursionUtil.getChildListTree(allDepts, "parentId", 0L, "deptId", "children", DeptVO.class);
    }

    @Override
    public void addDept(DeptAddBO bo) {
        ManagerDept dept = BeanUtil.copyProperties(bo, ManagerDept.class);
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }
        dept.setCreateTime(new Date());
        save(dept);
    }

    @Override
    public void updateDept(DeptUpdateBO bo) {
        ManagerDept dept = getById(bo.getDeptId());
        if (dept == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_SUCH_PARAMENT_ERROR);
        }
        if (bo.getDeptName() != null) {
            dept.setDeptName(bo.getDeptName());
        }
        if (bo.getParentId() != null) {
            dept.setParentId(bo.getParentId());
        }
        if (bo.getSortOrder() != null) {
            dept.setSortOrder(bo.getSortOrder());
        }
        updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long deptId) {
        // 检查是否有子部门
        long childCount = lambdaQuery().eq(ManagerDept::getParentId, deptId).count();
        if (childCount > 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "该部门下存在子部门，无法删除");
        }
        // 检查是否有关联用户
        long userCount = manageUserService.lambdaQuery().eq(ManagerUser::getDeptId, deptId).count();
        if (userCount > 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "该部门下存在成员，无法删除");
        }
        removeById(deptId);
    }
}
