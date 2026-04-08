package com.aquadev.aiexecutor.service.ai.homework;

import com.aquadev.aiexecutor.service.ai.strategy.AiProviderRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkContextBuilderImpl implements HomeworkContextBuilder {

    private final AiProviderRouter providerRouter;

    @Override
    public String buildFullContext(String ocrText, List<Media> images) {
        if (images == null || images.isEmpty()) {
            log.info("buildFullContext: no images, skipping vision");
            return ocrText;
        }
        log.info("buildFullContext: describing {} images via router", images.size());
        String imageDescriptions = providerRouter.describeImages(images);
        log.info("buildFullContext: image description done ({} chars)", imageDescriptions.length());
        return ocrText + "\n\n--- ATTACHED IMAGE DESCRIPTIONS ---\n" + imageDescriptions;
    }
}
