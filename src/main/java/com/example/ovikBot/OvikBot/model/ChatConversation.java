package com.example.ovikBot.OvikBot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent record of a single chat-mode conversation owned by a {@link User}.
 *
 * <p>
 * Messages are stored inlined as JSONB on a single column. That keeps the
 * schema flat for v1 and works well with Hibernate 6's {@link SqlTypes#JSON}
 * mapping. If we ever need per-message queries (regenerate, edit, delete-one),
 * a normalised {@code messages} table is the obvious next step.
 * </p>
 */
@Entity
@Table(name = "chat_conversations", indexes = {
        @Index(name = "idx_chat_conv_user_id", columnList = "user_id"),
        @Index(name = "idx_chat_conv_user_pinned_updated", columnList = "user_id, pinned, updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversation {

    /**
     * Opaque client-supplied id (e.g. {@code chat-1718000000000}). Kept as a
     * string so the frontend can keep generating the same ids it uses today.
     */
    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 64)
    private String id;

    /** Owner. Always the authenticated user's id. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "preview", nullable = false, length = 256)
    private String preview;

    @Column(name = "mode", nullable = false, length = 16)
    private String mode;

    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    /**
     * Full message history, serialised as JSONB. Each element matches the
     * shape returned by the frontend ({@code id, role, content, ...}).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "messages", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> messages = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Soft-delete marker. {@code null} means the row is live. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null)
            createdAt = now;
        updatedAt = now;
        if (mode == null || mode.isBlank())
            mode = "chat";
        if (title == null)
            title = "New chat";
        if (preview == null)
            preview = "";
        if (messages == null)
            messages = new ArrayList<>();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}