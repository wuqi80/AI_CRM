package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.WecomSuiteTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WecomSuiteTicketMapper extends BaseMapper<WecomSuiteTicket> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM crm_wecom_suite_ticket
            WHERE suite_id = #{suiteId}
            ORDER BY received_at DESC, update_time DESC
            LIMIT 1
            """)
    WecomSuiteTicket selectLatestBySuiteIdIgnoreTenant(@Param("suiteId") String suiteId);
}
