package com.example.ovikBot.OvikBot.controller;

import com.example.ovikBot.OvikBot.dto.ChatHistoryDto;
import com.example.ovikBot.OvikBot.dto.ChatHistoryUpsertRequest;
import com.example.ovikBot.OvikBot.repository.AuthenticatedUser;
import com.example.ovikBot.OvikBot.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatHistoryService service;

    @GetMapping
    public ResponseEntity<List<ChatHistoryDto>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        UUID userId = user.getId();
        return ResponseEntity.ok(service.listForUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatHistoryDto> getOne(@PathVariable String id, @AuthenticationPrincipal AuthenticatedUser user) {
        UUID userId = user.getId();

        return service.getOne(id, userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChatHistoryDto> upsert(
            @PathVariable String id,
            @RequestBody ChatHistoryUpsertRequest body, @AuthenticationPrincipal AuthenticatedUser user) {
        UUID userId = user.getId();

        return ResponseEntity.ok(service.upsert(id, userId, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal AuthenticatedUser user) {
        UUID userId = user.getId();

        return service.softDelete(id, userId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/pin")
    public ResponseEntity<ChatHistoryDto> togglePin(
            @PathVariable String id,
            @RequestBody PinRequest body, @AuthenticationPrincipal AuthenticatedUser user) {
        UUID userId = user.getId();

        return service.setPinned(id, userId, body.pinned())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }



    public record PinRequest(boolean pinned) {
    }
}