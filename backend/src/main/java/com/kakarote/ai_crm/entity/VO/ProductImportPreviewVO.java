package com.kakarote.ai_crm.entity.VO;

import com.kakarote.ai_crm.entity.BO.ProductImportBO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductImportPreviewVO {

    private int totalRows;
    private int validRows;
    private int errorRows;
    private int duplicateRows;
    private List<ProductImportBO> rows = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
}
