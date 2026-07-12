package com.example.ovikBot.OvikBot.controller;


import com.example.ovikBot.OvikBot.dto.ChatRequest;
import com.example.ovikBot.OvikBot.dto.ChatResponse;
import com.example.ovikBot.OvikBot.service.ChatService;
import com.example.ovikBot.OvikBot.service.UploadService;
import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Controller;
//import okhttp3.MediaType;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {


    private final ChatService chatService;
    private final UploadService uploadService;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request.getMessage());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestParam String message
    ) {
        return chatService.stream(message);
    }

    @PostMapping("/upload")
    ResponseEntity<String> upload(
            @RequestParam("file") MultipartFile file
    ) {
        uploadService.upload(file);
        return ResponseEntity.ok("File uploaded successfully.");
    }

    @PostMapping("/ask")
    ResponseEntity<String> ask(@RequestBody ChatRequest request) {
        String ans = chatService.askFromRag(request);
        return ResponseEntity.ok(ans);
    }

}
