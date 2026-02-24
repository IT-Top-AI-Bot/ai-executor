package com.aquadev.ittopaiexecutor.service.document.extractor;

import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.util.Map;
import java.util.Set;

@Component
public class PdfDocumentExtractor implements DocumentExtractor {

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of("pdf");

    @Override
    public Set<String> supportedMimeTypes() {
        return SUPPORTED_MIME_TYPES;
    }

    @Override
    public ExtractedDocument extract(byte[] content, String filename, ChatClient chatClient) {
        Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        String text = chatClient.prompt()
                .user(u -> u
                        .text("Extract and return the complete text content of this document. " +
                                "If there are images or screenshots — describe their content inline.")
                        .media(new Media(new MimeType("application", "pdf"), resource))
                )
                .call()
                .content();

        return new ExtractedDocument(text, Map.of("source", filename));
    }
}
