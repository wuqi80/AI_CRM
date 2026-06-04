package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.hutool.core.util.IdUtil;
import com.kakarote.ai_crm.entity.BO.WecomSyncRunBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.WecomConversation;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.entity.PO.WecomEmployee;
import com.kakarote.ai_crm.entity.PO.WecomExternalCustomer;
import com.kakarote.ai_crm.entity.PO.WecomMessage;
import com.kakarote.ai_crm.entity.PO.WecomSyncCursor;
import com.kakarote.ai_crm.entity.PO.WecomSyncLog;
import com.kakarote.ai_crm.entity.VO.WecomSyncStatusVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.mapper.ManagerDeptMapper;
import com.kakarote.ai_crm.mapper.WecomConversationMapper;
import com.kakarote.ai_crm.mapper.WecomEmployeeMapper;
import com.kakarote.ai_crm.mapper.WecomExternalCustomerMapper;
import com.kakarote.ai_crm.mapper.WecomMessageMapper;
import com.kakarote.ai_crm.mapper.WecomSyncCursorMapper;
import com.kakarote.ai_crm.mapper.WecomSyncLogMapper;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

@Service
public class WecomSyncServiceImpl {

    private static final String SYNC_STATUS_SUCCESS = "success";
    private static final String SYNC_STATUS_FAILED = "failed";
    private static final String CURSOR_ARCHIVE = "archive";
    private static final String CURSOR_KEY_MAIN = "main";
    private static final String CRM_CUSTOMER_STAGE_LEAD = "lead";
    private static final String CRM_CUSTOMER_SOURCE_WECOM = "企业微信";
    private static final String WECOM_BIND_STATUS_BOUND = "BOUND";

    @Autowired
    private WecomTokenService tokenService;

    @Autowired
    private WecomApiClient apiClient;

    @Autowired
    private WecomFinanceArchiveGateway archiveGateway;

    @Autowired
    private WecomMessageNormalizeService messageNormalizeService;

    @Autowired
    private WecomEmployeeMapper employeeMapper;

    @Autowired
    private WecomExternalCustomerMapper externalCustomerMapper;

    @Autowired
    private WecomConversationMapper conversationMapper;

    @Autowired
    private WecomMessageMapper messageMapper;

    @Autowired
    private WecomSyncCursorMapper cursorMapper;

    @Autowired
    private WecomSyncLogMapper syncLogMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private ManagerDeptMapper deptMapper;

    @Autowired
    private ManageUserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public WecomSyncStatusVO runSync(WecomCorpConfig config, WecomSyncRunBO runBO) {
        WecomSyncLog log = new WecomSyncLog();
        log.setCorpId(config.getCorpId());
        log.setSyncType("manual");
        log.setStatus("running");
        log.setFetchedCount(0);
        log.setSavedCount(0);
        log.setFailedCount(0);
        log.setStartedAt(new Date());
        syncLogMapper.insert(log);

        int fetched = 0;
        int saved = 0;
        int failed = 0;
        try {
            if (Boolean.TRUE.equals(runBO.getSyncEmployees())) {
                int orgSaved = syncOrganization(config);
                fetched += orgSaved;
                saved += orgSaved;
            }
            if (Boolean.TRUE.equals(runBO.getSyncCustomers())) {
                String token = tokenService.fetchContactAccessToken(config);
                List<String> followUsers = apiClient.listFollowUsers(token);
                for (String followUser : followUsers) {
                    List<String> externalUserIds = apiClient.listExternalUserIds(token, followUser);
                    fetched += externalUserIds.size();
                    for (String externalUserId : externalUserIds) {
                        saved += saveExternalCustomer(config, apiClient.getExternalCustomer(token, externalUserId),
                                resolveSyncOwnerId());
                    }
                }
            }
            if (Boolean.TRUE.equals(runBO.getSyncConversations())) {
                saved += syncArchiveMessages(config, runBO.getArchiveLimit());
            }
            log.setStatus(SYNC_STATUS_SUCCESS);
        } catch (Exception e) {
            failed++;
            log.setStatus(SYNC_STATUS_FAILED);
            log.setErrorMessage(e.getMessage());
        } finally {
            log.setFetchedCount(fetched);
            log.setSavedCount(saved);
            log.setFailedCount(failed);
            log.setFinishedAt(new Date());
            syncLogMapper.updateById(log);
        }

        WecomSyncStatusVO status = new WecomSyncStatusVO();
        status.setCorpId(config.getCorpId());
        status.setLastSyncTime(log.getFinishedAt());
        status.setLastSyncStatus(log.getStatus());
        status.setLastSyncError(log.getErrorMessage());
        status.setFetchedCount(fetched);
        status.setSavedCount(saved);
        status.setFailedCount(failed);
        return status;
    }

    public int syncVisibleCustomers(WecomCorpConfig config, String employeeUserId, Long ownerId) {
        if (StrUtil.isBlank(employeeUserId) || ownerId == null) {
            return 0;
        }
        String token = tokenService.fetchContactAccessToken(config);
        int saved = 0;
        for (String externalUserId : apiClient.listExternalUserIds(token, employeeUserId)) {
            saved += saveExternalCustomer(config, apiClient.getExternalCustomer(token, externalUserId), ownerId);
        }
        return saved;
    }

    public int syncOrganization(WecomCorpConfig config) {
        String token = tokenService.fetchAppAccessToken(config);
        List<JSONObject> departments = apiClient.listDepartments(token);
        Map<Long, Long> deptIdMap = new LinkedHashMap<>();
        int saved = 0;
        Date syncedAt = new Date();
        List<JSONObject> sortedDepartments = departments.stream()
                .filter(item -> item.getLong("id") != null)
                .sorted(Comparator.comparing((JSONObject item) -> item.getLongValue("parentid"))
                        .thenComparing(item -> item.getLongValue("id")))
                .toList();
        for (JSONObject department : sortedDepartments) {
            saved += saveDepartment(config, department, deptIdMap, syncedAt);
        }

        Set<String> syncedUsers = new HashSet<>();
        for (JSONObject department : sortedDepartments) {
            Long departmentId = department.getLong("id");
            for (JSONObject user : apiClient.listDepartmentUsers(token, departmentId)) {
                String userId = user.getString("userid");
                if (StrUtil.isBlank(userId) || !syncedUsers.add(userId)) {
                    continue;
                }
                saved += saveSystemUserAndEmployee(config, user, deptIdMap, syncedAt);
            }
        }
        return saved;
    }

    private int saveDepartment(WecomCorpConfig config, JSONObject department, Map<Long, Long> deptIdMap, Date syncedAt) {
        Long wecomDeptId = department.getLong("id");
        if (wecomDeptId == null) {
            return 0;
        }
        Long wecomParentDeptId = department.getLong("parentid");
        ManagerDept dept = deptMapper.selectOne(Wrappers.<ManagerDept>lambdaQuery()
                .eq(ManagerDept::getWecomCorpId, config.getCorpId())
                .eq(ManagerDept::getWecomDeptId, wecomDeptId)
                .last("LIMIT 1"));
        if (dept == null) {
            dept = new ManagerDept();
            dept.setCreateTime(syncedAt);
        }
        dept.setDeptName(StrUtil.blankToDefault(department.getString("name"), "WeCom Dept " + wecomDeptId));
        dept.setParentId(resolveParentDeptId(wecomParentDeptId, deptIdMap));
        dept.setSortOrder(department.getInteger("order"));
        dept.setWecomCorpId(config.getCorpId());
        dept.setWecomDeptId(wecomDeptId);
        dept.setWecomParentDeptId(wecomParentDeptId);
        dept.setWecomSyncedAt(syncedAt);
        if (dept.getDeptId() == null) {
            deptMapper.insert(dept);
        } else {
            deptMapper.updateById(dept);
        }
        deptIdMap.put(wecomDeptId, dept.getDeptId());
        return 1;
    }

    private int saveSystemUserAndEmployee(WecomCorpConfig config,
                                          JSONObject user,
                                          Map<Long, Long> deptIdMap,
                                          Date syncedAt) {
        String wecomUserId = user.getString("userid");
        if (StrUtil.isBlank(wecomUserId)) {
            return 0;
        }
        ManagerUser managerUser = findSystemUser(config.getCorpId(), wecomUserId, user);
        if (managerUser == null) {
            managerUser = new ManagerUser();
            managerUser.setUsername(resolveSystemUsername(config, user));
            managerUser.setPassword(encodeGeneratedPassword());
            managerUser.setCreateTime(syncedAt);
        }
        managerUser.setRealname(StrUtil.blankToDefault(user.getString("name"), wecomUserId));
        managerUser.setMobile(StrUtil.blankToDefault(user.getString("mobile"), managerUser.getMobile()));
        managerUser.setEmail(StrUtil.blankToDefault(user.getString("email"), managerUser.getEmail()));
        managerUser.setImg(StrUtil.blankToDefault(user.getString("avatar"), managerUser.getImg()));
        managerUser.setPost(StrUtil.blankToDefault(user.getString("position"), managerUser.getPost()));
        managerUser.setStatus(resolveManagerUserStatus(user.getInteger("status")));
        managerUser.setDeptId(resolveUserDeptId(user.getJSONArray("department"), deptIdMap));
        managerUser.setWecomCorpId(config.getCorpId());
        managerUser.setWecomUserId(wecomUserId);
        managerUser.setWecomSyncedAt(syncedAt);
        if (managerUser.getUserId() == null) {
            userMapper.insert(managerUser);
        } else {
            userMapper.updateById(managerUser);
        }
        saveWecomEmployee(config, user, managerUser.getUserId(), syncedAt);
        return 1;
    }

    private Long resolveParentDeptId(Long wecomParentDeptId, Map<Long, Long> deptIdMap) {
        if (wecomParentDeptId == null || wecomParentDeptId <= 0) {
            return 0L;
        }
        return deptIdMap.getOrDefault(wecomParentDeptId, 0L);
    }

    private Long resolveUserDeptId(JSONArray departments, Map<Long, Long> deptIdMap) {
        if (departments == null || departments.isEmpty()) {
            return null;
        }
        Long firstDeptId = departments.getLong(0);
        return firstDeptId == null ? null : deptIdMap.get(firstDeptId);
    }

    private ManagerUser findSystemUser(String corpId, String wecomUserId, JSONObject user) {
        ManagerUser managerUser = userMapper.selectOne(Wrappers.<ManagerUser>lambdaQuery()
                .eq(ManagerUser::getWecomCorpId, corpId)
                .eq(ManagerUser::getWecomUserId, wecomUserId)
                .last("LIMIT 1"));
        if (managerUser != null) {
            return managerUser;
        }
        String email = user.getString("email");
        if (StrUtil.isNotBlank(email)) {
            managerUser = userMapper.selectOne(Wrappers.<ManagerUser>lambdaQuery()
                    .eq(ManagerUser::getEmail, email)
                    .last("LIMIT 1"));
            if (managerUser != null) {
                return managerUser;
            }
        }
        String mobile = user.getString("mobile");
        if (StrUtil.isNotBlank(mobile)) {
            return userMapper.selectOne(Wrappers.<ManagerUser>lambdaQuery()
                    .eq(ManagerUser::getMobile, mobile)
                    .last("LIMIT 1"));
        }
        return null;
    }

    private String resolveSystemUsername(WecomCorpConfig config, JSONObject user) {
        String email = user.getString("email");
        if (StrUtil.isNotBlank(email)) {
            return StrUtil.trim(email).toLowerCase();
        }
        return syntheticWecomEmail(config.getCorpId(), user.getString("userid"));
    }

    private String encodeGeneratedPassword() {
        String password = "Wecom" + IdUtil.fastSimpleUUID().substring(0, 12);
        return passwordEncoder == null ? password : passwordEncoder.encode(password);
    }

    private int resolveManagerUserStatus(Integer wecomStatus) {
        return wecomStatus == null || wecomStatus == 1 ? 1 : 0;
    }

    private void saveWecomEmployee(WecomCorpConfig config, JSONObject user, Long crmUserId, Date syncedAt) {
        String userId = user.getString("userid");
        WecomEmployee employee = employeeMapper.selectOne(Wrappers.<WecomEmployee>lambdaQuery()
                .eq(WecomEmployee::getCorpId, config.getCorpId())
                .eq(WecomEmployee::getUserId, userId)
                .last("LIMIT 1"));
        if (employee == null) {
            employee = new WecomEmployee();
            employee.setCorpId(config.getCorpId());
            employee.setUserId(userId);
        }
        employee.setCrmUserId(crmUserId);
        employee.setName(StrUtil.blankToDefault(user.getString("name"), userId));
        employee.setAvatar(user.getString("avatar"));
        employee.setDepartmentList(jsonArrayToString(user.getJSONArray("department")));
        employee.setMobile(StrUtil.blankToDefault(user.getString("mobile"), employee.getMobile()));
        employee.setEmail(StrUtil.blankToDefault(user.getString("email"), employee.getEmail()));
        employee.setPosition(StrUtil.blankToDefault(user.getString("position"), employee.getPosition()));
        employee.setStatus(user.getInteger("status"));
        employee.setSyncedAt(syncedAt);
        if (employee.getId() == null) {
            employeeMapper.insert(employee);
        } else {
            employeeMapper.updateById(employee);
        }
    }

    private int saveEmployees(WecomCorpConfig config, List<JSONObject> employees) {
        int saved = 0;
        for (JSONObject user : employees) {
            String userId = user.getString("userid");
            if (StrUtil.isBlank(userId)) {
                continue;
            }
            WecomEmployee employee = employeeMapper.selectOne(Wrappers.<WecomEmployee>lambdaQuery()
                    .eq(WecomEmployee::getCorpId, config.getCorpId())
                    .eq(WecomEmployee::getUserId, userId)
                    .last("LIMIT 1"));
            if (employee == null) {
                employee = new WecomEmployee();
                employee.setCorpId(config.getCorpId());
                employee.setUserId(userId);
            }
            employee.setName(StrUtil.blankToDefault(user.getString("name"), userId));
            employee.setAvatar(user.getString("avatar"));
            employee.setDepartmentList(jsonArrayToString(user.getJSONArray("department")));
            employee.setMobile(StrUtil.blankToDefault(user.getString("mobile"), employee.getMobile()));
            employee.setEmail(StrUtil.blankToDefault(user.getString("email"), employee.getEmail()));
            employee.setPosition(StrUtil.blankToDefault(user.getString("position"), employee.getPosition()));
            employee.setStatus(user.getInteger("status"));
            employee.setSyncedAt(new Date());
            if (employee.getId() == null) {
                employeeMapper.insert(employee);
            } else {
                employeeMapper.updateById(employee);
            }
            saved++;
        }
        return saved;
    }

    private int saveExternalCustomer(WecomCorpConfig config, JSONObject response, Long ownerId) {
        JSONObject customerJson = response.getJSONObject("external_contact");
        if (customerJson == null) {
            return 0;
        }
        String externalUserId = customerJson.getString("external_userid");
        if (StrUtil.isBlank(externalUserId)) {
            return 0;
        }
        WecomExternalCustomer customer = externalCustomerMapper.selectOne(Wrappers.<WecomExternalCustomer>lambdaQuery()
                .eq(WecomExternalCustomer::getCorpId, config.getCorpId())
                .eq(WecomExternalCustomer::getExternalUserId, externalUserId)
                .last("LIMIT 1"));
        Date syncedAt = new Date();
        Long crmCustomerId = saveCrmCustomer(config, customerJson, externalUserId, customer, syncedAt, ownerId);
        if (customer == null) {
            customer = new WecomExternalCustomer();
            customer.setCorpId(config.getCorpId());
            customer.setExternalUserId(externalUserId);
        }
        customer.setBindStatus(WECOM_BIND_STATUS_BOUND);
        customer.setCustomerId(crmCustomerId);
        customer.setName(StrUtil.blankToDefault(customerJson.getString("name"), externalUserId));
        customer.setAvatar(customerJson.getString("avatar"));
        customer.setType(customerJson.getInteger("type"));
        customer.setGender(customerJson.getInteger("gender"));
        customer.setUnionId(customerJson.getString("unionid"));
        customer.setPosition(customerJson.getString("position"));
        customer.setCorpName(customerJson.getString("corp_name"));
        customer.setCorpFullName(customerJson.getString("corp_full_name"));
        customer.setExternalProfile(response.toJSONString());
        customer.setSyncedAt(syncedAt);
        if (customer.getId() == null) {
            externalCustomerMapper.insert(customer);
        } else {
            externalCustomerMapper.updateById(customer);
        }
        return 1;
    }

    private Long saveCrmCustomer(WecomCorpConfig config, JSONObject customerJson, String externalUserId,
                                 WecomExternalCustomer externalCustomer, Date syncedAt, Long ownerId) {
        Customer customer = findExistingCrmCustomer(config, externalUserId, externalCustomer);
        if (customer == null) {
            customer = new Customer();
            customer.setStage(CRM_CUSTOMER_STAGE_LEAD);
            customer.setOwnerId(ownerId);
            customer.setStatus(1);
            customer.setSource(CRM_CUSTOMER_SOURCE_WECOM);
            customer.setContactCount(1);
        } else {
            if (StrUtil.isBlank(customer.getStage())) {
                customer.setStage(CRM_CUSTOMER_STAGE_LEAD);
            }
            if (ownerId != null) {
                customer.setOwnerId(ownerId);
            } else if (customer.getOwnerId() == null) {
                customer.setOwnerId(resolveSyncOwnerId());
            }
            if (customer.getStatus() == null) {
                customer.setStatus(1);
            }
            if (customer.getContactCount() == null || customer.getContactCount() <= 0) {
                customer.setContactCount(1);
            }
        }
        customer.setCompanyName(resolveCrmCustomerName(customerJson, externalUserId));
        customer.setPrimaryContactName(StrUtil.blankToDefault(customerJson.getString("name"), externalUserId));
        customer.setPrimaryContactPosition(customerJson.getString("position"));
        customer.setSource(CRM_CUSTOMER_SOURCE_WECOM);
        customer.setWecomCustomer(true);
        customer.setWecomCorpId(config.getCorpId());
        customer.setWecomExternalUserId(externalUserId);
        customer.setWecomSyncedAt(syncedAt);
        if (customer.getCustomerId() == null) {
            customerMapper.insert(customer);
        } else {
            customerMapper.updateById(customer);
        }
        return customer.getCustomerId();
    }

    private Customer findExistingCrmCustomer(WecomCorpConfig config, String externalUserId,
                                             WecomExternalCustomer externalCustomer) {
        if (externalCustomer != null && externalCustomer.getCustomerId() != null) {
            Customer boundCustomer = customerMapper.selectById(externalCustomer.getCustomerId());
            if (boundCustomer != null) {
                return boundCustomer;
            }
        }
        return customerMapper.selectOne(Wrappers.<Customer>lambdaQuery()
                .eq(Customer::getWecomCustomer, true)
                .eq(Customer::getWecomCorpId, config.getCorpId())
                .eq(Customer::getWecomExternalUserId, externalUserId)
                .last("LIMIT 1"));
    }

    private String resolveCrmCustomerName(JSONObject customerJson, String externalUserId) {
        String corpFullName = customerJson.getString("corp_full_name");
        if (StrUtil.isNotBlank(corpFullName)) {
            return corpFullName;
        }
        String corpName = customerJson.getString("corp_name");
        if (StrUtil.isNotBlank(corpName)) {
            return corpName;
        }
        return StrUtil.blankToDefault(customerJson.getString("name"), externalUserId);
    }

    private Long resolveSyncOwnerId() {
        Long userId = UserUtil.getUserIdOrNull();
        return userId == null ? UserUtil.getSuperUserId() : userId;
    }

    private String syntheticWecomEmail(String corpId, String userId) {
        String safeCorp = sanitizeEmailPart(StrUtil.blankToDefault(corpId, "corp"));
        String safeUser = sanitizeEmailPart(StrUtil.blankToDefault(userId, "user"));
        return "wecom." + safeUser + "." + safeCorp + "@external.wecom.local";
    }

    private String sanitizeEmailPart(String value) {
        String sanitized = value.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
        return StrUtil.blankToDefault(sanitized, "unknown");
    }

    private int syncArchiveMessages(WecomCorpConfig config, Integer archiveLimit) {
        if (!Boolean.TRUE.equals(config.getArchiveEnabled())) {
            return 0;
        }
        WecomSyncCursor cursor = cursorMapper.selectOne(Wrappers.<WecomSyncCursor>lambdaQuery()
                .eq(WecomSyncCursor::getCorpId, config.getCorpId())
                .eq(WecomSyncCursor::getCursorType, CURSOR_ARCHIVE)
                .eq(WecomSyncCursor::getCursorKey, CURSOR_KEY_MAIN)
                .last("LIMIT 1"));
        long startSeq = cursor == null || cursor.getSeq() == null ? 0L : cursor.getSeq();
        List<JSONObject> messages = archiveGateway.fetchMessages(config, startSeq, resolveArchiveLimit(archiveLimit));
        long maxSeq = startSeq;
        int saved = 0;
        for (JSONObject raw : messages) {
            Long conversationId = ensureConversation(config, raw);
            WecomMessage message = messageNormalizeService.normalize(conversationId, raw);
            message.setCorpId(config.getCorpId());
            Long seq = raw.getLong("seq");
            if (seq != null) {
                message.setSeq(seq);
                maxSeq = Math.max(maxSeq, seq);
            }
            if (StrUtil.isNotBlank(message.getMsgId()) && messageMapper.selectCount(Wrappers.<WecomMessage>lambdaQuery()
                    .eq(WecomMessage::getMsgId, message.getMsgId())) == 0) {
                messageMapper.insert(message);
                saved++;
            }
        }
        if (!Objects.equals(maxSeq, startSeq)) {
            if (cursor == null) {
                cursor = new WecomSyncCursor();
                cursor.setCorpId(config.getCorpId());
                cursor.setCursorType(CURSOR_ARCHIVE);
                cursor.setCursorKey(CURSOR_KEY_MAIN);
                cursor.setSeq(maxSeq);
                cursorMapper.insert(cursor);
            } else {
                cursor.setSeq(maxSeq);
                cursorMapper.updateById(cursor);
            }
        }
        return saved;
    }

    private Long ensureConversation(WecomCorpConfig config, JSONObject raw) {
        String from = raw.getString("from");
        JSONArray toList = raw.getJSONArray("tolist");
        String firstTo = toList == null || toList.isEmpty() ? "" : toList.getString(0);
        String conversationKey = from + ":" + firstTo;
        WecomConversation conversation = conversationMapper.selectOne(Wrappers.<WecomConversation>lambdaQuery()
                .eq(WecomConversation::getCorpId, config.getCorpId())
                .eq(WecomConversation::getChatId, conversationKey)
                .last("LIMIT 1"));
        if (conversation == null) {
            conversation = new WecomConversation();
            conversation.setCorpId(config.getCorpId());
            conversation.setConversationType(firstTo.startsWith("wm") ? "customer" : "employee");
            conversation.setEmployeeUserId(from);
            conversation.setExternalUserId(firstTo.startsWith("wm") ? firstTo : null);
            conversation.setChatId(conversationKey);
            conversation.setTitle(conversationKey);
            conversation.setOwnerUserId(null);
            conversation.setMessageCount(0);
            conversationMapper.insert(conversation);
        }
        conversation.setLastMsgId(raw.getString("msgid"));
        conversation.setLastMsgTime(raw.getLong("msgtime") == null ? null : new Date(raw.getLong("msgtime")));
        conversation.setLastMsgPreview(messageNormalizeService.normalize(conversation.getId(), raw).getContentText());
        conversation.setMessageCount(conversation.getMessageCount() == null ? 1 : conversation.getMessageCount() + 1);
        conversationMapper.updateById(conversation);
        return conversation.getId();
    }

    private String jsonArrayToString(JSONArray array) {
        return array == null ? "[]" : array.toJSONString();
    }

    private int resolveArchiveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, 1000);
    }
}
