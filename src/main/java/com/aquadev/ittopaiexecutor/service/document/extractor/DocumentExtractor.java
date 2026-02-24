package com.aquadev.ittopaiexecutor.service.document.extractor;

import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Set;

public interface DocumentExtractor {

    Set<String> supportedMimeTypes();

    ExtractedDocument extract(byte[] content, String filename, ChatClient chatClient);
}
