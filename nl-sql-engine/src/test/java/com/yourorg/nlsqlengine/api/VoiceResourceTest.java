package com.yourorg.nlsqlengine.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class VoiceResourceTest {

    // --- /api/voice/speak ---

    @Test
    void speak_withEmptyText_returns400orError() {
        given()
                .contentType("application/json")
                .body("{\"text\":\"\"}")
                .when()
                .post("/api/voice/speak")
                .then()
                .statusCode(anyOf(is(400), is(500)));
    }

    @Test
    void speak_withNoBody_returns400or500() {
        given()
                .contentType("application/json")
                .when()
                .post("/api/voice/speak")
                .then()
                .statusCode(anyOf(is(400), is(500)));
    }

    @Test
    @Tag("integration")
    void speak_withValidText_returnsWavAudio() {
        byte[] audio = given()
                .contentType("application/json")
                .body("{\"text\":\"Bonjour\"}")
                .when()
                .post("/api/voice/speak")
                .then()
                .statusCode(200)
                .contentType("audio/wav")
                .extract()
                .asByteArray();

        // WAV files start with "RIFF" header
        assertTrue(audio.length > 44, "WAV should contain audio data beyond header");
        assertEquals("RIFF", new String(audio, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    @Tag("integration")
    void speak_withLongText_returnsLargerWav() {
        byte[] shortAudio = given()
                .contentType("application/json")
                .body("{\"text\":\"Oui\"}")
                .when()
                .post("/api/voice/speak")
                .then()
                .statusCode(200)
                .extract()
                .asByteArray();

        byte[] longAudio = given()
                .contentType("application/json")
                .body("{\"text\":\"Quels sont les personnages de Star Wars originaires de la planète Tatooine ?\"}")
                .when()
                .post("/api/voice/speak")
                .then()
                .statusCode(200)
                .extract()
                .asByteArray();

        assertTrue(longAudio.length > shortAudio.length,
                "Longer text should produce larger audio");
    }

    @Test
    @Tag("integration")
    void speak_withSpecialCharacters_returnsWav() {
        given()
                .contentType("application/json")
                .body("{\"text\":\"L'étoile de la mort a été détruite. C'est \\\"incroyable\\\" !\"}")
                .when()
                .post("/api/voice/speak")
                .then()
                .statusCode(200)
                .contentType("audio/wav");
    }

    // --- /api/voice/transcribe ---

    @Test
    void transcribe_withNoFile_returns400or500() {
        given()
                .contentType("multipart/form-data")
                .when()
                .post("/api/voice/transcribe")
                .then()
                .statusCode(anyOf(is(400), is(500)));
    }

    @Test
    @Tag("integration")
    void transcribe_withWavFile_returnsJsonWithText() {
        // First generate a WAV via TTS
        byte[] wav = given()
                .contentType("application/json")
                .body("{\"text\":\"Quels sont les personnages de Tatooine\"}")
                .when()
                .post("/api/voice/speak")
                .then()
                .statusCode(200)
                .extract()
                .asByteArray();

        // Then transcribe it
        given()
                .multiPart("file", "test.wav", wav, "audio/wav")
                .when()
                .post("/api/voice/transcribe")
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("text", notNullValue());
    }

    // --- buildMultipart unit test via reflection ---

    @Test
    void buildMultipart_containsRequiredParts() throws Exception {
        VoiceResource resource = new VoiceResource();
        var method = VoiceResource.class.getDeclaredMethod("buildMultipart", String.class, String.class, byte[].class);
        method.setAccessible(true);

        byte[] fileContent = "fake audio".getBytes();
        byte[] result = (byte[]) method.invoke(resource, "test-boundary", "recording.webm", fileContent);
        String body = new String(result, StandardCharsets.UTF_8);

        assertTrue(body.contains("name=\"file\""), "Should contain file part");
        assertTrue(body.contains("filename=\"recording.webm\""), "Should contain filename");
        assertTrue(body.contains("name=\"model\""), "Should contain model part");
        assertTrue(body.contains("Systran/faster-whisper-medium"), "Should specify whisper model");
        assertTrue(body.contains("name=\"language\""), "Should contain language part");
        assertTrue(body.contains("fr"), "Should specify French language");
        assertTrue(body.contains("--test-boundary--"), "Should end with closing boundary");
    }
}
