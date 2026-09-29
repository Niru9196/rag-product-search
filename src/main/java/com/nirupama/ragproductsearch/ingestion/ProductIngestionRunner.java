package com.nirupama.ragproductsearch.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirupama.ragproductsearch.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads products.json into the vector store on startup.
 * <p>
 * Each document is tagged with a {@code catalogVersion} (SHA-256 of products.json). If documents with the
 * current version already exist, ingestion is skipped. Otherwise all existing documents are deleted and the
 * catalog is re-ingested, so editing products.json is enough to refresh the store on the next startup.
 */
@Component
public class ProductIngestionRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductIngestionRunner.class);

    /** Upper bound for fetching existing documents to delete; far above the catalog size. */
    private static final int MAX_EXISTING_DOCUMENTS = 10_000;

    private final VectorStore vectorStore;
    private final Resource productsFile;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProductIngestionRunner(VectorStore vectorStore,
                                  @Value("classpath:/products.json") Resource productsFile) {
        this.vectorStore = vectorStore;
        this.productsFile = productsFile;
    }

    @Override
    public void run(String... args) throws Exception {
        byte[] catalogBytes;
        try (InputStream in = productsFile.getInputStream()) {
            catalogBytes = in.readAllBytes();
        }
        String catalogVersion = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(catalogBytes));

        if (isAlreadyIngested(catalogVersion)) {
            log.info("Vector store already has catalog version {} — skipping ingestion.", catalogVersion);
            return;
        }

        deleteExistingDocuments();

        Product[] products = mapper.readValue(catalogBytes, Product[].class);
        List<Document> documents = Arrays.stream(products)
                .map(p -> toDocument(p, catalogVersion))
                .toList();

        vectorStore.add(documents);
        log.info("Ingested {} products into the vector store (catalog version {}).", documents.size(), catalogVersion);
    }

    private boolean isAlreadyIngested(String catalogVersion) {
        List<Document> current = vectorStore.similaritySearch(SearchRequest.builder()
                .query("product")
                .topK(1)
                .filterExpression("catalogVersion == '" + catalogVersion + "'")
                .build());
        return current != null && !current.isEmpty();
    }

    private void deleteExistingDocuments() {
        List<Document> existing = vectorStore.similaritySearch(SearchRequest.builder()
                .query("product")
                .topK(MAX_EXISTING_DOCUMENTS)
                .similarityThresholdAll()
                .build());
        if (existing == null || existing.isEmpty()) {
            return;
        }
        List<String> ids = existing.stream().map(Document::getId).toList();
        vectorStore.delete(ids);
        log.info("Deleted {} outdated documents from the vector store.", ids.size());
    }

    private static Document toDocument(Product p, String catalogVersion) {
        String text = String.format(Locale.ROOT,
                "%s. Category: %s. %s Fabric: %s. Fit: %s. Colors: %s. Tags: %s. Price: $%.2f",
                p.name(), p.category(), p.description(), p.fabric(), p.fit(),
                join(p.colors()), join(p.tags()), p.price() != null ? p.price() : 0.0);

        Map<String, Object> metadata = new HashMap<>();
        putIfNotNull(metadata, "id", p.id());
        putIfNotNull(metadata, "name", p.name());
        putIfNotNull(metadata, "category", p.category());
        putIfNotNull(metadata, "price", p.price());
        putIfNotNull(metadata, "image", p.image());
        metadata.put("catalogVersion", catalogVersion);

        return new Document(text, metadata);
    }

    private static String join(List<String> values) {
        return values == null ? "" : String.join(", ", values);
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
