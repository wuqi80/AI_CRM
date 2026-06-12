package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.BO.ProductCategoryAddBO;
import com.kakarote.ai_crm.entity.BO.ProductCategoryMoveBO;
import com.kakarote.ai_crm.entity.BO.ProductCategoryUpdateBO;
import com.kakarote.ai_crm.entity.PO.ProductCategory;
import com.kakarote.ai_crm.entity.VO.ProductCategoryVO;

import java.util.List;

public interface IProductCategoryService extends IService<ProductCategory> {

    Long ensureDefaultCategoryId();

    Long addCategory(ProductCategoryAddBO bo);

    void updateCategory(ProductCategoryUpdateBO bo);

    void moveCategory(ProductCategoryMoveBO bo);

    void deleteCategory(Long categoryId);

    List<ProductCategoryVO> tree();

    Long findCategoryIdByPath(String categoryPath);
}
