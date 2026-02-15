package com.yourorg.nlsqlengine.api;

import com.yourorg.nlsqlengine.rag.SchemaEmbeddingService;
import com.yourorg.nlsqlengine.rag.SwapiImporter;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/api/import")
@Produces(MediaType.APPLICATION_JSON)
public class ImportResource {

    @Inject
    SwapiImporter importer;

    @Inject
    SchemaEmbeddingService embeddingService;

    @POST
    public Response importData() {
        try {
            importer.importAll();
            return Response.ok(Map.of("status", "Import Star Wars terminé")).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("status", "error", "message", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/embeddings")
    public Response ingestEmbeddings() {
        try {
            embeddingService.purgeEmbeddings();
            embeddingService.ingestSchema();
            return Response.ok(Map.of("status", "Embeddings ingérés avec succès")).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("status", "error", "message", e.getMessage()))
                    .build();
        }
    }
}
