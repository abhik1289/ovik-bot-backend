package com.example.ovikBot.OvikBot.service;

import com.example.ovikBot.OvikBot.dto.ChatHistoryDto;
import com.example.ovikBot.OvikBot.dto.ChatHistoryUpsertRequest;
import com.example.ovikBot.OvikBot.model.ChatConversation;
import com.example.ovikBot.OvikBot.repository.ChatConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owner-scoped chat history CRUD.
 *
 * <p>
 * Every public method takes the authenticated user's id explicitly and
 * scopes the query to that user. We deliberately do <strong>not</strong> leak
 * existence: a missing or foreign-owned row returns {@link Optional#empty()}
 * and the controller turns that into {@code 404}, not {@code 403}.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatConversationRepository repository;

    @Transactional(readOnly = true)
    public List<ChatHistoryDto> listForUser(UUID userId) {
        return repository
                .findAllByUserIdAndDeletedAtIsNullOrderByPinnedDescUpdatedAtDesc(userId)
                .stream()
                .map(ChatHistoryDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ChatHistoryDto> getOne(String id, UUID userId) {
        return repository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .map(ChatHistoryDto::fromEntity);
    }

    /**
     * Insert or overwrite a conversation owned by {@code userId}.
     *
     * <p>
     * If the row exists and is owned by {@code userId} we update it in
     * place; soft-deleted rows are resurrected (the frontend treats that as
     * "undelete", which matches user intent).
     * </p>
     */
    @Transactional
    public ChatHistoryDto upsert(String id, UUID userId, ChatHistoryUpsertRequest req) {
        ChatConversation entity = repository.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .orElseGet(() -> ChatConversation.builder()
                        .id(id)
                        .userId(userId)
                        .build());

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        if (req.title() != null)
            entity.setTitle(req.title());
        if (req.preview() != null)
            entity.setPreview(req.preview());
        if (req.mode() != null && !req.mode().isBlank())
            entity.setMode(req.mode());
        if (req.pinned() != null)
            entity.setPinned(req.pinned());
        entity.setMessages(req.messages() != null ? new ArrayList<>(req.messages()) : new ArrayList<>());
        entity.setDeletedAt(null); // resurrect if previously soft-deleted

        ChatConversation saved = repository.save(entity);
        return ChatHistoryDto.fromEntity(saved);
    }

    /**
     * Returns {@code true} when a live, owned row was deleted. Soft-deletes are
     * idempotent; re-deleting an already-deleted row simply returns false.
     */
    @Transactional
    public boolean softDelete(String id, UUID userId) {
        return repository.softDelete(id, userId, Instant.now()) > 0;
    }

    @Transactional
    public Optional<ChatHistoryDto> setPinned(String id, UUID userId, boolean pinned) {
        return repository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .map(c -> {
                    c.setPinned(pinned);
                    return ChatHistoryDto.fromEntity(repository.save(c));
                });
    }
}