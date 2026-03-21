package com.aquadev.ittopaiexecutor.dto;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Objects;

public record SolveRequest(
        byte[] content,
        String filename,
        Long specId,
        String theme,
        String teacherFio,
        String nameSpec,
        String comment
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SolveRequest(byte[] c, String fn, Long sid, String th, String tfio, String ns, String cm)))
            return false;
        return Arrays.equals(content, c)
                && Objects.equals(filename, fn)
                && Objects.equals(specId, sid)
                && Objects.equals(theme, th)
                && Objects.equals(teacherFio, tfio)
                && Objects.equals(nameSpec, ns)
                && Objects.equals(comment, cm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(content), filename, specId, theme, teacherFio, nameSpec, comment);
    }

    @NonNull
    @Override
    public String toString() {
        return "SolveRequest[content=" + Arrays.toString(content)
                + ", filename=" + filename
                + ", specId=" + specId
                + ", theme=" + theme
                + ", teacherFio=" + teacherFio
                + ", nameSpec=" + nameSpec
                + ", comment=" + comment + "]";
    }
}
