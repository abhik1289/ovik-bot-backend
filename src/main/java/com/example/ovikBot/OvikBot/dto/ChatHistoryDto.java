package com.example.ovikBot.OvikBot.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Wire-format of a conversation returned to the frontend.
 *
 * <p>
 * Messages are kept as opaque JSON-shaped maps so the frontend can
 * reconstruct its own {@code Message} type without an extra DTO layer.
 * </p>
 */
public record ChatHistoryDto(
        String id,
        String title,
        String preview,
        String mode,
        boolean pinned,
        List<Map<String, Object>> messages,
        Instant createdAt,
        Instant updatedAt) {
    public static ChatHistoryDto fromEntity(
            com.example.ovikBot.OvikBot.model.ChatConversation c) {
        return new ChatHistoryDto(
                c.getId(),
                c.getTitle(),
                c.getPreview(),
                c.getMode(),
                c.isPinned(),
                c.getMessages(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}