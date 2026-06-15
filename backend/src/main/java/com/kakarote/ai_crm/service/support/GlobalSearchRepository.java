package com.kakarote.ai_crm.service.support;

import com.kakarote.ai_crm.entity.VO.GlobalSearchResultVO;
import com.kakarote.ai_crm.mapper.ContactMapper;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.KnowledgeMapper;
import com.kakarote.ai_crm.mapper.ScheduleMapper;
import com.kakarote.ai_crm.mapper.TaskMapper;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GlobalSearchRepository {

    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final TaskMapper taskMapper;
    private final ScheduleMapper scheduleMapper;
    private final KnowledgeMapper knowledgeMapper;

    public long count(String type, String keyword, String pattern) {
        Long count = switch (type) {
            case "customer" -> customerMapper.countGlobalSearch(keyword, pattern);
            case "contact" -> contactMapper.countGlobalSearch(keyword, pattern);
            case "task" -> taskMapper.countGlobalSearch(keyword, pattern);
            case "schedule" -> countMySchedules(keyword, pattern);
            case "knowledge" -> knowledgeMapper.countGlobalSearch(keyword, pattern);
            default -> 0L;
        };
        return count == null ? 0 : count;
    }

    public List<GlobalSearchResultVO> search(String type, String keyword, String pattern, int limit) {
        return switch (type) {
            case "customer" -> customerMapper.globalSearch(keyword, pattern, limit);
            case "contact" -> contactMapper.globalSearch(keyword, pattern, limit);
            case "task" -> taskMapper.globalSearch(keyword, pattern, limit);
            case "schedule" -> searchMySchedules(keyword, pattern, limit);
            case "knowledge" -> knowledgeMapper.globalSearch(keyword, pattern, limit);
            default -> List.of();
        };
    }

    private long countMySchedules(String keyword, String pattern) {
        Long userId = UserUtil.getUserId();
        if (userId == null) {
            return 0;
        }
        Long count = scheduleMapper.countGlobalSearch(keyword, pattern, userId);
        return count == null ? 0 : count;
    }

    private List<GlobalSearchResultVO> searchMySchedules(String keyword, String pattern, int limit) {
        Long userId = UserUtil.getUserId();
        if (userId == null) {
            return List.of();
        }
        return scheduleMapper.globalSearch(keyword, pattern, userId, limit);
    }
}
