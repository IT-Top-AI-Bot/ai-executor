package com.aquadev.ittopaiexecutor.service.file;

/**
 * Result of downloading a homework file into memory.
 *
 * @param content  raw bytes of the file
 * @param filename original filename with extension (e.g. "task.pdf", "lab1.docx")
 */
public record DownloadedFile(byte[] content, String filename) {
}
