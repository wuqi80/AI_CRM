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

    /**
     * 会话存档事件推送/拉取目标：按 (corp_id 或 archive_corp_id) 匹配，仅已授权且启用存档的企业（跨租户）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM crm_wecom_corp_config
            WHERE auth_status = 'AUTHORIZED'
              AND archive_enabled = TRUE
              AND (corp_id = #{corpId} OR archive_corp_id = #{corpId})
            """)
    List<WecomCorpConfig> selectArchiveTargetsByCorpIdIgnoreTenant(@Param("corpId") String corpId);
}
