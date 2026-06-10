package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

public interface WecomFinanceSdkClient {

    String SKIP_MESSAGE_FIELD = "__skip_archive_message";
    String SKIP_REASON_FIELD = "__skip_archive_reason";
    String SKIP_REASON_PUBLIC_KEY_VERSION_MISMATCH = "public_key_version_mismatch";

    List<JSONObject> fetchMessages(FetchRequest request);

    record FetchRequest(String corpId,
                        String secret,
                        String privateKey,
                        String publicKeyVersion,
                        long startSeq,
                        int limit) {
    }
}
