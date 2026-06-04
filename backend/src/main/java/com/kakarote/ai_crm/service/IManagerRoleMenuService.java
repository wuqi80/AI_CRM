package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.BO.RolePermissionSaveBO;
import com.kakarote.ai_crm.entity.PO.ManagerRoleMenu;

import java.util.List;

/**
 * <p>
 * 角色菜单对应关系表 服务类
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
public interface IManagerRoleMenuService extends IService<ManagerRoleMenu> {

    /**
     * 保存角色枚举
     *
     * @param roleId:角色id
     * @param menuIdList:menuIdList
     * @return
     */
    public void saveRoleMenu(Long roleId, List<Long> menuIdList);

    /**
     * 通过角色查询角色菜单对应关系
     * @param roleId 角色ID
     * @return
     */
    public List<ManagerRoleMenu> queryRoleMenuByRoleId(Long roleId);

    /**
     * 通过角色ID查询菜单列表
     * @param id
     * @return
     */
    List<Long> queryMenuIdListByRoleId(Long id);

    /**
     * 查询角色菜单关联（含数据范围）
     * @param roleId 角色ID
     * @return 角色菜单列表
     */
    List<ManagerRoleMenu> queryRoleMenuWithScopeByRoleId(Long roleId);

    /**
     * 保存角色菜单关联（含数据范围）
     * @param roleId 角色ID
     * @param items 权限项列表
     */
    void saveRoleMenuWithScope(Long roleId, List<RolePermissionSaveBO.PermItem> items);
}
