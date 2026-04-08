package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.GlobalSearchQueryBO;
import com.kakarote.ai_crm.entity.PO.GlobalSearchIndex;
import com.kakarote.ai_crm.entity.VO.GlobalSearchResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GlobalSearchIndexMapper extends BaseMapper<GlobalSearchIndex> {

    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    void upsert(@Param("index") GlobalSearchIndex index);

    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    void deleteByEntity(@Param("tenantId") Long tenantId,
                        @Param("entityType") String entityType,
                        @Param("entityId") Long entityId);

    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    void deleteByCustomerIdAndEntityType(@Param("tenantId") Long tenantId,
                                         @Param("customerId") Long customerId,
                                         @Param("entityType") String entityType);

    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    Long queryPageCount(@Param("query") GlobalSearchQueryBO query);

    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    IPage<GlobalSearchResultVO> queryPageList(IPage<GlobalSearchResultVO> page,
                                              @Param("query") GlobalSearchQueryBO query);
}
