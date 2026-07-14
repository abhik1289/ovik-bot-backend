package com.example.ovikBot.OvikBot.service;

import com.example.ovikBot.OvikBot.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStoreService vectorStoreService;
    private static final String DEFAULT_CONVERSATION_ID = "anonymous";
    private static final int RAG_TOP_K = 5;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a helpful assistant. Provide clear, accurate, and concise responses.
            """;

    private static final String RAG_SYSTEM_PROMPT = """
            You are a helpful RAG assistant.
            Answer only from the provided context when the context is relevant.
            If the answer is not present in the context, say that the uploaded documents do not contain enough information.
            Keep the answer clear, accurate, and concise.
            """;

    public ChatResponse chat(String message) {
        validateMessage(message);

        log.debug("Processing chat request: {}", truncateForLog(message));

        try {
            String response = chatClient.prompt()
                    .system(DEFAULT_SYSTEM_PROMPT)
                    .user(message)
                    .advisors(advisors ->
                            advisors.param(ChatMemory.CONVERSATION_ID, resolveConversationId())
                    )
                    .call()
                    .content();

            String safeResponse = response != null ? response : "I couldn't generate a response. Please try again.";

            log.debug("Chat request completed successfully");


            return ChatResponse.builder()
                    .response(safeResponse)
                    .build();


        } catch (Exception e) {
            log.error("Error processing chat request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process chat request", e);
        }
    }

    public Flux<String> stream(String message) {
        validateMessage(message);
        log.debug("Processing stream request: {}", truncateForLog(message));
        try {
            return chatClient.prompt()
                    .system(DEFAULT_SYSTEM_PROMPT)
                    .user(message)
                    .advisors((advisors) ->
                            advisors.param(ChatMemory.CONVERSATION_ID, resolveConversationId())
                    )
                    .stream()
                    .content();
        } catch (Exception e) {
            log.error("Error processing stream request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process stream request", e);
        }
    }

    private void validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or blank");
        }
    }

    private String truncateForLog(String input) {
        return input.length() > 100 ? input.substring(0, 100) + "..." : input;
    }

    public ChatResponse askFromRag(String message) {
        validateMessage(message);
        log.debug("Processing RAG request: {}", truncateForLog(message));

        List<Document> relevantDocuments = vectorStoreService.search(message, RAG_TOP_K);
        if (relevantDocuments == null || relevantDocuments.isEmpty()) {
            return ChatResponse.builder()
                    .response("No relevant document chunks were found. Upload a PDF before asking RAG questions.")
                    .build();
        }

        String context = buildContext(relevantDocuments);
        String ragPrompt = """
                Question:
                %s

                Context:
                %s
                """.formatted(message, context);

        try {
            String answer = chatClient.prompt()
                    .system(RAG_SYSTEM_PROMPT)
                    .user(ragPrompt)
                    .advisors(advisors ->
                            advisors.param(ChatMemory.CONVERSATION_ID, resolveConversationId())
                    )
                    .call()
                    .content();

            return ChatResponse.builder()
                    .response(answer != null ? answer : "I couldn't generate a RAG response. Please try again.")
                    .build();
        } catch (Exception e) {
            log.error("Error processing RAG request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process RAG request", e);
        }
    }

    private String buildContext(List<Document> documents) {
        return documents.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String resolveConversationId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return DEFAULT_CONVERSATION_ID;
        }

        return authentication.getName();
    }
}
