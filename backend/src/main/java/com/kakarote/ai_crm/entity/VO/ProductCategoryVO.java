package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class ProductCategoryVO {

    private Long categoryId;
    private Long parentId;
    private String categoryName;
    private String categoryPath;
    private Integer level;
    private Integer sortOrder;
    private Integer status;
    private Date createTime;
    private Date updateTime;
    private List<ProductCategoryVO> children = new ArrayList<>();
}
