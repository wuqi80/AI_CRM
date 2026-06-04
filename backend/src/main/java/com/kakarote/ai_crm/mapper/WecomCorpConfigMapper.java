package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WecomCorpConfigMapper extends BaseMapper<WecomCorpConfig> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM crm_wecom_corp_config
            WHERE corp_id = #{corpId}
              AND auth_status = 'AUTHORIZED'
            ORDER BY update_time DESC
            LIMIT 1
            """)
    WecomCorpConfig selectAuthorizedThirdPartyByCorpIdIgnoreTenant(@Param("corpId") String corpId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM crm_wecom_corp_config
            WHERE corp_id = #{corpId}
            ORDER BY update_time DESC
            """)
    List<WecomCorpConfig> selectThirdPartyByCorpIdIgnoreTenant(@Param("corpId") String corpId);
}
