package org.acme.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.acme.api.ApiPaths;
import org.acme.model.dto.ChatRequest;
import org.acme.model.dto.ChatResponse;
import org.acme.service.AiService;

import jakarta.inject.Inject;

@Path(ApiPaths.CHAT)
public class AiResource {

    @Inject
    AiService aiService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ChatResponse chat(ChatRequest request) {

        String response = aiService.ask(
                request.message(),
                request.documentIds()
        );
        return new ChatResponse(response);
    }
}

