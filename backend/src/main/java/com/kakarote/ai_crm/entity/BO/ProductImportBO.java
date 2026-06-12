package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductImportBO {

    private Integer rowNum;
    private String productName;
    private String productCode;
    private String categoryPath;
    private Long categoryId;
    private String productType;
    private String unit;
    private BigDecimal standardPrice;
    private BigDecimal costPrice;
    private String ownerName;
    private Long ownerId;
    private String status;
    private String description;
    private boolean duplicate;
    private Long existingProductId;
    private String handleMode;
    private List<String> errors = new ArrayList<>();
}
