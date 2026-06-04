package com.kakarote.syncdata.mq;

import com.kakarote.syncdata.SyncProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RocketMqSyncSettingsTest {

    @Test
    void usesUnifiedTopicAndDefaultDirectionTags() {
        SyncProperties properties = new SyncProperties();

        assertThat(RocketMqSyncSettings.topic(properties)).isEqualTo("crm-aicrm-sync-events");
        assertThat(RocketMqSyncSettings.crmToAicrmTag(properties)).isEqualTo("crm-to-aicrm");
        assertThat(RocketMqSyncSettings.aicrmToCrmTag(properties)).isEqualTo("aicrm-to-crm");
        assertThat(RocketMqSyncSettings.crmToAicrmGroup(properties)).isEqualTo("sync-data-crm-to-aicrm");
        assertThat(RocketMqSyncSettings.aicrmToCrmGroup(properties)).isEqualTo("sync-data-aicrm-to-crm");
    }

    @Test
    void customUnifiedTopicDoesNotDependOnDeprecatedDirectionalTopics() {
        SyncProperties properties = new SyncProperties();
        properties.getRocketmq().setTopic("shared-sync-topic");
        properties.getRocketmq().getCrmToAicrm().setTopic("old-crm-topic");
        properties.getRocketmq().getAicrmToCrm().setTopic("old-aicrm-topic");
        properties.getRocketmq().getCrmToAicrm().setTag("crm-custom-tag");
        properties.getRocketmq().getAicrmToCrm().setTag("aicrm-custom-tag");

        assertThat(RocketMqSyncSettings.topic(properties)).isEqualTo("shared-sync-topic");
        assertThat(RocketMqSyncSettings.crmToAicrmTag(properties)).isEqualTo("crm-custom-tag");
        assertThat(RocketMqSyncSettings.aicrmToCrmTag(properties)).isEqualTo("aicrm-custom-tag");
    }
}
