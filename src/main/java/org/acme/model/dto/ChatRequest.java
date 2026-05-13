package org.acme.model.dto;

import java.util.List;
import java.util.UUID;

public record ChatRequest(
        String message,
        List<UUID> documentIds) {
}
