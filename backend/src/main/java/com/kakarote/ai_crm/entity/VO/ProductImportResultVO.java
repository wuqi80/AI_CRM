package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductImportResultVO {

    private int imported;
    private int updated;
    private int skipped;
    private List<String> errors = new ArrayList<>();
}
