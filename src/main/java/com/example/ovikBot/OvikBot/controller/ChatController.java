package com.example.ovikBot.OvikBot.controller;

import com.example.ovikBot.OvikBot.dto.ChatRequest;
import com.example.ovikBot.OvikBot.dto.ChatResponse;
import com.example.ovikBot.OvikBot.service.ChatService;
import com.example.ovikBot.OvikBot.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UploadService uploadService;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request.getMessage());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestParam String message,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        log.info("Received request to stream message: {}", message);
        return chatService.stream(message)
                .onErrorResume(err -> Flux.just(
                        "event: error\ndata: " + err.getMessage().replace("\n", " ") + "\n\n"));
    }

    @PostMapping("/rag/upload")
    public ResponseEntity<String> uploadRagPdf(@RequestParam("file") MultipartFile file) {
        uploadService.upload(file);
        return ResponseEntity.ok("PDF uploaded and indexed successfully.");
    }

    @PostMapping("/rag/ask")
    public ChatResponse askFromRag(@RequestBody ChatRequest request) {
        return chatService.askFromRag(request.getMessage());
    }
}
