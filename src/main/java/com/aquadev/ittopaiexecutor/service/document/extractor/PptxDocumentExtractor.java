package com.aquadev.ittopaiexecutor.service.document.extractor;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class PptxDocumentExtractor extends BaseOfficeDocumentExtractor {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pptx", "ppt");

    @Override
    public Set<String> supportedMimeTypes() {
        return SUPPORTED_EXTENSIONS;
    }

    @Override
    protected String formatName() {
        return "PPTX";
    }

    @Override
    protected List<Resource> extractImageResources(byte[] content, String filename) {
        if (filename.toLowerCase().endsWith(".ppt")) {
            // OLE2 format — XMLSlideShow is OOXML only
            log.debug("Skipping image extraction for OLE2 .ppt file: {}", filename);
            return List.of();
        }
        try (XMLSlideShow pptx = new XMLSlideShow(new ByteArrayInputStream(content))) {
            return pptx.getPictureData().stream()
                    .map(pic -> (Resource) new ByteArrayResource(pic.getData()) {
                        @Override
                        public String getFilename() {
                            return pic.getFileName();
                        }
                    })
                    .toList();
        } catch (IOException e) {
            log.warn("Could not extract images from PPTX: {}", e.getMessage());
            return List.of();
        }
    }
}
