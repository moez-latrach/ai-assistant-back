package org.acme.model.dto;

import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String name

) {
}
