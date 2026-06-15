package com.kakarote.ai_crm.service;

import org.springframework.web.multipart.MultipartFile;

public interface AiAudioTranscriptionService {

    String transcribe(MultipartFile audioFile);

    String transcribe(byte[] audioBytes, String filename, String contentType);
}
