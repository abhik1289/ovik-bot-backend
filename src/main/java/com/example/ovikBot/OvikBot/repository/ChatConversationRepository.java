package com.example.ovikBot.OvikBot.repository;

import com.example.ovikBot.OvikBot.model.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatConversationRepository
        extends JpaRepository<ChatConversation, String> {

    /**
     * List live conversations for a user, pinned first then most-recent.
     * Soft-deleted rows are filtered out at the query level.
     */
    List<ChatConversation> findAllByUserIdAndDeletedAtIsNullOrderByPinnedDescUpdatedAtDesc(UUID userId);

    /** Owner-scoped fetch that refuses to leak other users' rows. */
    Optional<ChatConversation> findByIdAndUserIdAndDeletedAtIsNull(String id, UUID userId);

    @Modifying
    @Query("UPDATE ChatConversation c SET c.deletedAt = :now " +
            "WHERE c.id = :id AND c.userId = :userId AND c.deletedAt IS NULL")
    int softDelete(@Param("id") String id,
            @Param("userId") UUID userId,
            @Param("now") Instant now);
}