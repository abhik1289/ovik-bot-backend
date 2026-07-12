package com.example.ovikBot.OvikBot.service;


import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChunkService {

    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public List<Document> chunk(List<Document> documents) {
        return splitter.apply(documents);
    }

}
