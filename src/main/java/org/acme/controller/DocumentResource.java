package org.acme.controller;


import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.acme.api.ApiPaths;
import org.acme.model.dto.DocumentResponse;
import org.acme.model.dto.UploadResponse;
import org.acme.service.DocumentFileService;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import java.util.List;


@Path(ApiPaths.DOCUMENT)
public class DocumentResource {

    private static final Logger LOG = Logger.getLogger(DocumentResource.class);
    @Inject
    DocumentFileService documentService;

    @POST
    @Path(ApiPaths.UPLOAD)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@RestForm FileUpload file) throws Exception {
        UploadResponse response =
                documentService.upload(file);
        return Response.ok(response).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<DocumentResponse> findAll() {

        return documentService.findAll();

    }
}
