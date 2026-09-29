package com.nirupama.ragproductsearch.dto;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SearchResultTest {

    @Test
    void mapsAllMetadataFields() {
        Document doc = Document.builder()
                .text("Crepe Wrap Midi Dress. ...")
                .metadata(Map.of(
                        "id", 2,
                        "name", "Crepe Wrap Midi Dress",
                        "category", "women",
                        "price", 85.0,
                        "image", "/images/product_2.png"))
                .score(0.87)
                .build();

        SearchResult result = SearchResult.fromDocument(doc);

        assertThat(result).isEqualTo(new SearchResult(
                2, "Crepe Wrap Midi Dress", "women", 85.0, "/images/product_2.png",
                "Crepe Wrap Midi Dress. ...", 0.87));
    }

    @Test
    void acceptsNumbersStoredAsOtherNumericTypes() {
        // pgvector metadata is JSON, so numbers may come back as Long/Double/Integer
        Document doc = Document.builder()
                .text("x")
                .metadata(Map.of("id", 7L, "price", 38))
                .build();

        SearchResult result = SearchResult.fromDocument(doc);

        assertThat(result.id()).isEqualTo(7);
        assertThat(result.price()).isEqualTo(38.0);
    }

    @Test
    void missingMetadataGivesNullsAndZeroScore() {
        Document doc = Document.builder().text("old document").build();

        SearchResult result = SearchResult.fromDocument(doc);

        assertThat(result.id()).isNull();
        assertThat(result.name()).isNull();
        assertThat(result.image()).isNull();
        assertThat(result.price()).isNull();
        assertThat(result.content()).isEqualTo("old document");
        assertThat(result.score()).isZero();
    }
}
