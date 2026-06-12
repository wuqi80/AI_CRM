package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Data
public class ProductVO {

    private Long productId;
    private String productName;
    private String productCode;
    private String mainImage;
    private String mainImageUrl;
    private Long categoryId;
    private String categoryName;
    private String categoryPath;
    private String productType;
    private String unit;
    private BigDecimal standardPrice;
    private BigDecimal costPrice;
    private Long ownerId;
    private String ownerName;
    private String status;
    private String description;
    private Map<String, Object> customFields;
    private Long createUserId;
    private String createUserName;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
}
