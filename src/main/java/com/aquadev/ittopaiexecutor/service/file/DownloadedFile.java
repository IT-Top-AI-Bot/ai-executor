package com.aquadev.ittopaiexecutor.service.file;

import org.jspecify.annotations.NonNull;

public record DownloadedFile(byte[] content, String filename) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DownloadedFile(byte[] otherContent, String otherFilename))) return false;
        return java.util.Arrays.equals(content, otherContent) && java.util.Objects.equals(filename, otherFilename);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(java.util.Arrays.hashCode(content), filename);
    }

    @NonNull
    @Override
    public String toString() {
        return "DownloadedFile[content=" + java.util.Arrays.toString(content) + ", filename=" + filename + "]";
    }
}
