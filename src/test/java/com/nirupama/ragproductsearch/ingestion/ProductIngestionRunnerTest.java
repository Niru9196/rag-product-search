package com.nirupama.ragproductsearch.ingestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductIngestionRunnerTest {

    private VectorStore vectorStore;
    private ProductIngestionRunner runner;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        runner = new ProductIngestionRunner(vectorStore, new ClassPathResource("products.json"));
    }

    /** The "is the current catalog version already ingested?" query uses a filter expression. */
    private void currentVersionSearchReturns(List<Document> docs) {
        when(vectorStore.similaritySearch(argThat((SearchRequest r) -> r != null && r.hasFilterExpression())))
                .thenReturn(docs);
    }

    /** The "fetch everything to delete" query has no filter. */
    private void allDocumentsSearchReturns(List<Document> docs) {
        when(vectorStore.similaritySearch(argThat((SearchRequest r) -> r != null && !r.hasFilterExpression())))
                .thenReturn(docs);
    }

    @Test
    void skipsWhenCurrentCatalogVersionIsAlreadyIngested() throws Exception {
        currentVersionSearchReturns(List.of(new Document("existing", Map.of())));

        runner.run();

        verify(vectorStore, never()).add(anyList());
        verify(vectorStore, never()).delete(anyList());
    }

    @Test
    void replacesOldDocumentsWhenCatalogChanged() throws Exception {
        currentVersionSearchReturns(List.of());
        allDocumentsSearchReturns(List.of(
                new Document("old-1", "old jacket", Map.of()),
                new Document("old-2", "old boots", Map.of())));

        runner.run();

        verify(vectorStore).delete(List.of("old-1", "old-2"));
        assertAdded36ProductDocuments();
    }

    @Test
    void ingestsWithoutDeletingWhenStoreIsEmpty() throws Exception {
        currentVersionSearchReturns(List.of());
        allDocumentsSearchReturns(List.of());

        runner.run();

        verify(vectorStore, never()).delete(anyList());
        assertAdded36ProductDocuments();
    }

    @SuppressWarnings("unchecked")
    private void assertAdded36ProductDocuments() {
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        List<Document> added = captor.getValue();

        assertThat(added).hasSize(36);
        assertThat(added).allSatisfy(doc -> {
            Map<String, Object> meta = doc.getMetadata();
            assertThat(meta.get("image")).isEqualTo("/images/product_" + meta.get("id") + ".png");
            assertThat((String) meta.get("catalogVersion")).isNotBlank();
            assertThat(meta).containsKeys("name", "category", "price");
        });

        Document first = added.stream().filter(d -> Integer.valueOf(1).equals(d.getMetadata().get("id")))
                .findFirst().orElseThrow();
        assertThat(first.getText())
                .contains("Striped Flutter Sleeve Peplum Blouse", "Category: women", "Fabric: Viscose crepe",
                        "Tags: blouse, striped, occasion", "Price: $50.00");
    }
}
