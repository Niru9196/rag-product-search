package com.nirupama.ragproductsearch.service;

import com.nirupama.ragproductsearch.dto.SearchResponse;
import com.nirupama.ragproductsearch.dto.SearchResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private static final String SYSTEM_PROMPT = """
        You are a product search assistant. You will be given a user's query and a set of
        retrieved product listings, ordered from most to least relevant.

        Before answering, first identify the constraints stated in the user's query:
        - Product type (e.g. "jacket", "boots", "pants") — if none is stated, note that.
        - Price limit — if none is stated, note that.
        - Use case or category (e.g. "hiking", "everyday wear") — if none is stated, note that.

        Then apply these rules:
        1. If the user's query names a specific product type, you MUST only recommend a product
           whose name or description literally contains that product type — a shared broader
           category (like "Outdoor") is NOT sufficient to satisfy this constraint.
        2. Among products matching the required product type, prioritize the highest-ranked
           (most relevant) one that also satisfies the price and use-case constraints.
        3. If no retrieved product matches the requested product type, say so explicitly rather
           than recommending a different type of product as a substitute.
        4. Only mention products that were actually retrieved — never invent products.

        Structure your answer as:
        Constraints identified: <list them>
        Recommendation: <your answer, following the rules above>
        """;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public SearchService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .build();
    }

    public List<SearchResult> searchProductsRaw(String query) {
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

    public SearchResponse searchWithSynthesis(String query) {
        List<SearchResult> matchedProducts = searchProductsRaw(query);

        String answer = chatClient.prompt()
                .user(query)
                .call()
                .content();

        return new SearchResponse(answer, matchedProducts);
    }
}