package com.nirupama.ragproductsearch.service;

import com.nirupama.ragproductsearch.dto.SearchResult;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private final VectorStore vectorStore;

    public SearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<SearchResult> searchProducts(String query) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(5)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);

        return results.stream()
                .map(doc -> new SearchResult(
                        doc.getText(),
                        doc.getScore() != null ? doc.getScore() : 0.0
                ))
                .toList();
    }
}