package com.example.ovikBot.OvikBot.service;


import com.example.ovikBot.OvikBot.dto.ChatRequest;
import com.example.ovikBot.OvikBot.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

//import org.springframework.ai.chat.client.advisor.;
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStoreService vectorStoreService;
//    private final ChatMemory chatMemory;
    private static final String DEFAULT_CONVERSATION_ID = "user-1";
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
                            advisors.param(ChatMemory.CONVERSATION_ID, DEFAULT_CONVERSATION_ID)
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
                    .user(message)
                    .advisors((advisors) ->
                            advisors.param(ChatMemory.CONVERSATION_ID, DEFAULT_CONVERSATION_ID)
                    )
                    .stream()
                    .content();

//            return res;

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

    public String askFromRag(ChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        validateMessage(request.getMessage());

        List<Document> relevantDocuments = vectorStoreService.search(request.getMessage(), RAG_TOP_K);
        if (relevantDocuments == null || relevantDocuments.isEmpty()) {
            return "No relevant document chunks were found. Upload a document before asking RAG questions.";
        }

        String context = buildContext(relevantDocuments);
        String ragPrompt = """
                Question:
                %s

                Context:
                %s
                """.formatted(request.getMessage(), context);

        String answer = chatClient.prompt()
                .system(RAG_SYSTEM_PROMPT)
                .user(ragPrompt)
                .advisors(advisors ->
                        advisors.param(ChatMemory.CONVERSATION_ID, DEFAULT_CONVERSATION_ID)
                )
                .call()
                .content();

        return answer != null ? answer : "I couldn't generate a RAG response. Please try again.";
    }

    private String buildContext(List<Document> documents) {
        return documents.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n\n---\n\n"));

    }
}
