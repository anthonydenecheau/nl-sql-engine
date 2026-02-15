package com.yourorg.nlsqlengine.api;

import java.time.LocalDateTime;

public record SavedPrompt(Long id, Long domainId, String question, String sqlGenerated, int usageCount, LocalDateTime createdAt) {
}
