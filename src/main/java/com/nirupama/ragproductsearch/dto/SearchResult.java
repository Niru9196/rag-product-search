package com.nirupama.ragproductsearch.dto;

import org.springframework.ai.document.Document;

import java.util.Map;

/**
 * A single matched product returned to the frontend.
 *
 * @param image path to the product image on this backend, e.g. "/images/product_1.png"
 */
public record SearchResult(
        Integer id,
        String name,
        String category,
        Double price,
        String image,
        String content,
        double score
) {

    /** Builds a result from a vector-store document, tolerating missing or differently-typed metadata. */
    public static SearchResult fromDocument(Document doc) {
        Map<String, Object> meta = doc.getMetadata();
        return new SearchResult(
                asInteger(meta.get("id")),
                asString(meta.get("name")),
                asString(meta.get("category")),
                asDouble(meta.get("price")),
                asString(meta.get("image")),
                doc.getText(),
                doc.getScore() != null ? doc.getScore() : 0.0
        );
    }

    private static Integer asInteger(Object value) {
        return value instanceof Number n ? n.intValue() : null;
    }

    private static Double asDouble(Object value) {
        return value instanceof Number n ? n.doubleValue() : null;
    }

    private static String asString(Object value) {
        return value instanceof String s ? s : null;
    }
}
