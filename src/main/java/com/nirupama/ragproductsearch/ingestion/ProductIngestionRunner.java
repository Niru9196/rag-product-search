package com.nirupama.ragproductsearch.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirupama.ragproductsearch.model.Product;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProductIngestionRunner implements CommandLineRunner {

    private final VectorStore vectorStore;

    @Value("classpath:/products.json")
    private Resource productsFile;

    public ProductIngestionRunner(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {
        List<org.springframework.ai.document.Document> existing = vectorStore.similaritySearch(
                SearchRequest.builder().query("product").topK(1).build()
        );

        if (!existing.isEmpty()) {
            System.out.println("Vector store already has data — skipping ingestion.");
            return;
        };

        ObjectMapper mapper = new ObjectMapper();
        Product[] products = mapper.readValue(productsFile.getInputStream(), Product[].class);

        List<Document> documents = List.of(products).stream()
                .map(p -> new Document(
                        p.getName() + ". " + p.getDescription() + ". Category: " + p.getCategory() + ". Price: $" + p.getPrice(),
                        Map.of("name", p.getName(), "category", p.getCategory(), "price", p.getPrice())
                ))
                .toList();

        vectorStore.add(documents);
        System.out.println("Ingested " + documents.size() + " products into the vector store.");
    }
}