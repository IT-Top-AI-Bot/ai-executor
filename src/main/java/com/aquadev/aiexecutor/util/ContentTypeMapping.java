package com.aquadev.aiexecutor.util;

import java.util.Map;

public interface ContentTypeMapping {
    Map<String, String> extensionToContentType();

    Map<String, String> contentTypeToExtension();
}
