package com.yourorg.nlsqlengine.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/domains")
@Produces(MediaType.APPLICATION_JSON)
public class DomainResource {

    @Inject
    DomainRepository domainRepository;

    @Inject
    DomainTableRepository domainTableRepository;

    @GET
    public List<Domain> list() {
        return domainRepository.findAll();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(Domain domain) {
        if (domain.name() == null || domain.name().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Le nom est obligatoire").build();
        }
        Domain created = domainRepository.create(domain.name().strip(), domain.description());
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    @Path("/{id}/tables")
    public List<String> tables(@PathParam("id") long id) {
        return domainTableRepository.findTablesByDomain(id);
    }

    @PUT
    @Path("/{id}/tables/{tableName}")
    public Response associate(@PathParam("id") long id, @PathParam("tableName") String tableName) {
        domainTableRepository.associate(id, tableName);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}/tables/{tableName}")
    public Response dissociate(@PathParam("id") long id, @PathParam("tableName") String tableName) {
        domainTableRepository.dissociate(id, tableName);
        return Response.noContent().build();
    }
}
