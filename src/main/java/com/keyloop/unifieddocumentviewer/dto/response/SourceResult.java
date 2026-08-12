package com.keyloop.unifieddocumentviewer.dto.response;

import java.util.List;

import com.keyloop.unifieddocumentviewer.constants.SourceStatus;
import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;

public record SourceResult(String source, SourceStatus status, List<UnifiedDocument> documents) {

    public static SourceResult success(String source, List<UnifiedDocument> documents) {
        return new SourceResult(source, SourceStatus.SUCCESS, documents == null ? List.of() : List.copyOf(documents));
    }

    public static SourceResult failed(String source) {
        return new SourceResult(source, SourceStatus.FAILED, List.of());
    }

    public boolean failed() {
        return status == SourceStatus.FAILED;
    }
}
