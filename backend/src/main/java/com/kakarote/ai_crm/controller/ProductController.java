package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.ProductAddBO;
import com.kakarote.ai_crm.entity.BO.ProductImportBO;
import com.kakarote.ai_crm.entity.BO.ProductQueryBO;
import com.kakarote.ai_crm.entity.BO.ProductSettingsUpdateBO;
import com.kakarote.ai_crm.entity.BO.ProductStatusUpdateBO;
import com.kakarote.ai_crm.entity.BO.ProductTransferBO;
import com.kakarote.ai_crm.entity.BO.ProductUpdateBO;
import com.kakarote.ai_crm.entity.VO.ProductImportPreviewVO;
import com.kakarote.ai_crm.entity.VO.ProductImportResultVO;
import com.kakarote.ai_crm.entity.VO.ProductSettingsVO;
import com.kakarote.ai_crm.entity.VO.ProductVO;
import com.kakarote.ai_crm.service.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/product")
@Tag(name = "Product")
public class ProductController {

    private final IProductService productService;

    public ProductController(IProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/add")
    @Operation(summary = "Create product")
    public Result<Long> addProduct(@Valid @RequestBody ProductAddBO bo) {
        return Result.ok(productService.addProduct(bo));
    }

    @PostMapping("/update")
    @Operation(summary = "Update product")
    public Result<Void> updateProduct(@Valid @RequestBody ProductUpdateBO bo) {
        productService.updateProduct(bo);
        return Result.ok();
    }

    @PostMapping("/delete/{productId}")
    @Operation(summary = "Delete product")
    public Result<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return Result.ok();
    }

    @PostMapping("/status")
    @Operation(summary = "Update product status")
    public Result<Void> updateStatus(@Valid @RequestBody ProductStatusUpdateBO bo) {
        productService.updateStatus(bo);
        return Result.ok();
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer product owner")
    public Result<Void> transferProducts(@Valid @RequestBody ProductTransferBO bo) {
        productService.transferProducts(bo);
        return Result.ok();
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "Query product page")
    public Result<BasePage<ProductVO>> queryPageList(@RequestBody ProductQueryBO queryBO) {
        return Result.ok(productService.queryPageList(queryBO));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Product detail")
    public Result<ProductVO> detail(@PathVariable Long productId) {
        return Result.ok(productService.getProductDetail(productId));
    }

    @GetMapping("/settings")
    @Operation(summary = "Product settings")
    public Result<ProductSettingsVO> getSettings() {
        return Result.ok(productService.getSettings());
    }

    @PostMapping("/settings")
    @Operation(summary = "Update product settings")
    public Result<Void> updateSettings(@RequestBody ProductSettingsUpdateBO bo) {
        productService.updateSettings(bo);
        return Result.ok();
    }

    @PostMapping("/export")
    @Operation(summary = "Export products")
    public void exportProducts(@RequestBody ProductQueryBO queryBO, HttpServletResponse response) {
        productService.exportProducts(queryBO, response);
    }

    @GetMapping("/import/template")
    @Operation(summary = "Download product import template")
    public void downloadImportTemplate(HttpServletResponse response) {
        productService.downloadImportTemplate(response);
    }

    @PostMapping("/import/preview")
    @Operation(summary = "Preview product import")
    public Result<ProductImportPreviewVO> importPreview(@RequestParam("file") MultipartFile file) {
        return Result.ok(productService.importPreview(file));
    }

    @PostMapping("/import/confirm")
    @Operation(summary = "Confirm product import")
    public Result<ProductImportResultVO> confirmImport(@RequestBody List<ProductImportBO> rows) {
        return Result.ok(productService.confirmImport(rows));
    }
}
