package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.VO.DeptVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ManagerDeptMapper extends BaseMapper<ManagerDept> {

    /**
     * 查询所有部门及其下用户数量
     *
     * @return 部门列表（含 userCount）
     */
    List<DeptVO> queryAllDeptWithUserCount();
}
