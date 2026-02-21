package com.yourorg.nlsqlengine.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.UUID;

@Path("/api/voice")
public class VoiceResource {

    @ConfigProperty(name = "voice.whisper.url", defaultValue = "http://whisper:8000")
    String whisperUrl;

    @ConfigProperty(name = "voice.piper.url", defaultValue = "http://piper:5000")
    String piperUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @POST
    @Path("/transcribe")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response transcribe(@RestForm("file") FileUpload file) throws IOException, InterruptedException {
        byte[] audioBytes = Files.readAllBytes(file.uploadedFile());
        String boundary = UUID.randomUUID().toString();

        byte[] multipartBody = buildMultipart(boundary, file.fileName(), audioBytes);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(whisperUrl + "/v1/audio/transcriptions"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return Response.status(response.statusCode())
                .type(MediaType.APPLICATION_JSON)
                .entity(response.body())
                .build();
    }

    @POST
    @Path("/speak")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("audio/wav")
    public Response speak(SpeakRequest speakRequest) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(piperUrl + "/synthesize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"text\":\"" + speakRequest.text().replace("\"", "\\\"") + "\"}"))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        return Response.status(response.statusCode())
                .type("audio/wav")
                .entity(response.body())
                .build();
    }

    public record SpeakRequest(String text) {}

    private byte[] buildMultipart(String boundary, String filename, byte[] fileBytes) {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String model = "\r\n--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"model\"\r\n\r\n"
                + "Systran/faster-whisper-medium";
        String language = "\r\n--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"language\"\r\n\r\n"
                + "fr";
        String footer = "\r\n--" + boundary + "--\r\n";

        byte[] headerBytes = header.getBytes();
        byte[] modelBytes = model.getBytes();
        byte[] languageBytes = language.getBytes();
        byte[] footerBytes = footer.getBytes();

        byte[] result = new byte[headerBytes.length + fileBytes.length + modelBytes.length + languageBytes.length + footerBytes.length];
        int pos = 0;
        System.arraycopy(headerBytes, 0, result, pos, headerBytes.length); pos += headerBytes.length;
        System.arraycopy(fileBytes, 0, result, pos, fileBytes.length); pos += fileBytes.length;
        System.arraycopy(modelBytes, 0, result, pos, modelBytes.length); pos += modelBytes.length;
        System.arraycopy(languageBytes, 0, result, pos, languageBytes.length); pos += languageBytes.length;
        System.arraycopy(footerBytes, 0, result, pos, footerBytes.length);
        return result;
    }
}
