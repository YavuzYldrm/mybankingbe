package com.mybanking.app.common.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        String path,
        int status,
        String code,
        String message,
        List<String> details,
        String traceId
) {}
