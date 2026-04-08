package com.aquadev.aiexecutor.util;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultContentTypeMapping implements ContentTypeMapping {

    @Override
    public Map<String, String> extensionToContentType() {
        return Map.ofEntries(
                Map.entry("html", "text/html; charset=utf-8"),
                Map.entry("htm", "text/html; charset=utf-8"),
                Map.entry("css", "text/css; charset=utf-8"),
                Map.entry("js", "application/javascript"),
                Map.entry("ts", "application/typescript"),
                Map.entry("py", "text/x-python; charset=utf-8"),
                Map.entry("java", "text/x-java-source; charset=utf-8"),
                Map.entry("txt", "text/plain; charset=utf-8"),
                Map.entry("md", "text/markdown; charset=utf-8"),
                Map.entry("json", "application/json"),
                Map.entry("xml", "application/xml"),
                Map.entry("csv", "text/csv; charset=utf-8"),
                Map.entry("zip", "application/zip"),
                // Documents
                Map.entry("pdf", "application/pdf"),
                Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                Map.entry("doc", "application/msword"),
                Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
                Map.entry("ppt", "application/vnd.ms-powerpoint"),
                Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                Map.entry("xls", "application/vnd.ms-excel"),
                Map.entry("epub", "application/epub+zip"),
                Map.entry("rtf", "application/rtf"),
                Map.entry("odt", "application/vnd.oasis.opendocument.text"),
                Map.entry("bib", "application/x-bibtex"),
                Map.entry("fb2", "application/x-fictionbook+xml"),
                Map.entry("ipynb", "application/x-ipynb+json"),
                Map.entry("tex", "application/x-tex"),
                Map.entry("opml", "text/x-opml"),
                // Images
                Map.entry("png", "image/png"),
                Map.entry("jpg", "image/jpeg"),
                Map.entry("jpeg", "image/jpeg"),
                Map.entry("gif", "image/gif"),
                Map.entry("webp", "image/webp"),
                Map.entry("bmp", "image/bmp"),
                Map.entry("tiff", "image/tiff"),
                Map.entry("tif", "image/tiff"),
                Map.entry("avif", "image/avif"),
                Map.entry("heic", "image/heic"),
                Map.entry("heif", "image/heif")
        );
    }

    @Override
    public Map<String, String> contentTypeToExtension() {
        return Map.ofEntries(
                Map.entry("application/pdf", "pdf"),
                Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
                Map.entry("application/msword", "doc"),
                Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
                Map.entry("application/vnd.ms-powerpoint", "ppt"),
                Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
                Map.entry("application/vnd.ms-excel", "xls"),
                Map.entry("text/csv", "csv"),
                Map.entry("application/epub+zip", "epub"),
                Map.entry("application/rtf", "rtf"),
                Map.entry("text/rtf", "rtf"),
                Map.entry("application/vnd.oasis.opendocument.text", "odt"),
                Map.entry("application/xml", "xml"),
                Map.entry("text/xml", "xml"),
                Map.entry("text/plain", "txt"),
                Map.entry("image/png", "png"),
                Map.entry("image/jpeg", "jpg"),
                Map.entry("image/gif", "gif"),
                Map.entry("image/webp", "webp"),
                Map.entry("image/bmp", "bmp"),
                Map.entry("image/tiff", "tiff"),
                Map.entry("image/avif", "avif"),
                Map.entry("image/heic", "heic"),
                Map.entry("image/heif", "heif")
        );
    }
}
