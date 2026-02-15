package com.yourorg.nlsqlengine.api;

import com.yourorg.nlsqlengine.orchestration.NlSqlOrchestrator;
import com.yourorg.nlsqlengine.orchestration.OrchestratorResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/query")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class QueryResource {

    @Inject
    NlSqlOrchestrator orchestrator;

    @POST
    public Response query(QueryRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(QueryResponse.error(null, "La question est obligatoire"))
                    .build();
        }

        try {
            OrchestratorResult result = orchestrator.process(request.question(), request.domainId());

            QueryResponse response = new QueryResponse(
                    result.question(),
                    result.generatedSql(),
                    result.results(),
                    result.answer(),
                    result.error()
            );

            if (result.isSuccess()) {
                return Response.ok(response).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(response).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(QueryResponse.error(request.question(), e.getMessage()))
                    .build();
        }
    }
}
