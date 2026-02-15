package com.yourorg.nlsqlengine.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/prompts")
@Produces(MediaType.APPLICATION_JSON)
public class PromptResource {

    @Inject
    SavedPromptRepository promptRepository;

    @GET
    public List<SavedPrompt> byDomain(@QueryParam("domain") Long domainId) {
        if (domainId == null) {
            return List.of();
        }
        return promptRepository.findByDomain(domainId);
    }

    @GET
    @Path("/popular")
    public List<SavedPrompt> popular(@QueryParam("domain") Long domainId, @QueryParam("limit") @DefaultValue("5") int limit) {
        return promptRepository.findPopular(domainId, limit);
    }

    @GET
    @Path("/search")
    public List<SavedPrompt> search(@QueryParam("domain") Long domainId, @QueryParam("q") String query) {
        return promptRepository.search(domainId, query);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response save(SavedPrompt prompt) {
        if (prompt.question() == null || prompt.question().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("La question est obligatoire").build();
        }
        SavedPrompt saved = promptRepository.save(prompt);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        boolean deleted = promptRepository.deleteById(id);
        return deleted ? Response.noContent().build() : Response.status(Response.Status.NOT_FOUND).build();
    }

    @PUT
    @Path("/{id}/increment")
    public Response increment(@PathParam("id") long id) {
        promptRepository.incrementUsage(id);
        return Response.ok().build();
    }
}
