package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.BO.ProductAddBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.Product;
import com.kakarote.ai_crm.entity.PO.ProductCategory;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.mapper.ProductMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.IProductCategoryService;
import com.kakarote.ai_crm.service.ISystemConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private IProductCategoryService productCategoryService;

    @Mock
    private ManageUserMapper manageUserMapper;

    @Mock
    private ISystemConfigService systemConfigService;

    @Mock
    private ICustomFieldService customFieldService;

    @Mock
    private FileStorageService fileStorageService;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(
                productMapper,
                productCategoryService,
                manageUserMapper,
                systemConfigService,
                customFieldService,
                fileStorageService
        );
        setCurrentUser(1001L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addProductDefaultsCategoryOwnerTypeStatusAndDelFlag() {
        ProductCategory category = new ProductCategory();
        category.setCategoryId(2001L);
        category.setCategoryName("Default");
        category.setDelFlag(0);
        category.setLevel(1);
        when(productCategoryService.ensureDefaultCategoryId()).thenReturn(2001L);
        when(productCategoryService.getById(2001L)).thenReturn(category);
        when(productMapper.countByCode("SKU-1", null)).thenReturn(0L);
        when(manageUserMapper.getUserId(1001L)).thenReturn(activeUser(1001L));

        ProductAddBO addBO = new ProductAddBO();
        addBO.setProductName("CRM License");
        addBO.setProductCode(" SKU-1 ");

        productService.addProduct(addBO);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertThat(saved.getProductName()).isEqualTo("CRM License");
        assertThat(saved.getProductCode()).isEqualTo("SKU-1");
        assertThat(saved.getCategoryId()).isEqualTo(2001L);
        assertThat(saved.getOwnerId()).isEqualTo(1001L);
        assertThat(saved.getProductType()).isEqualTo("goods");
        assertThat(saved.getStatus()).isEqualTo("active");
        assertThat(saved.getDelFlag()).isZero();
        assertThat(saved.getCreateUserId()).isEqualTo(1001L);
        assertThat(saved.getUpdateUserId()).isEqualTo(1001L);
    }

    @Test
    void getVisibleProductRejectsDeletedProduct() {
        Product product = new Product();
        product.setProductId(3001L);
        product.setProductName("Deleted");
        product.setDelFlag(1);
        when(productMapper.selectById(3001L)).thenReturn(product);

        assertThatThrownBy(() -> productService.getVisibleProduct(3001L))
                .isInstanceOf(BusinessException.class);
    }

    private ManagerUser activeUser(Long userId) {
        ManagerUser user = new ManagerUser();
        user.setUserId(userId);
        user.setUsername("user" + userId);
        user.setStatus(1);
        return user;
    }

    private void setCurrentUser(Long userId) {
        ManagerUser managerUser = activeUser(userId);
        LoginUser loginUser = new LoginUser();
        loginUser.setUser(managerUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities())
        );
    }
}
