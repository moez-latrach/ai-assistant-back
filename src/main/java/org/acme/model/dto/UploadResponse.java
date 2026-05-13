package org.acme.model.dto;


import java.util.UUID;

public record UploadResponse(

        UUID id,

        String filename

) {
}
