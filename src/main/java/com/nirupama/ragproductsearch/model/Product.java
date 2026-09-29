package com.nirupama.ragproductsearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A catalog product as defined in products.json. Products are only stored in the
 * vector store, so this is a plain value object (not a JPA entity).
 *
 * @param image path served by the backend, e.g. "/images/product_1.png"
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Product(
        Integer id,
        String name,
        String category,
        Double price,
        Double was,
        List<String> colors,
        String fabric,
        String fit,
        Double rating,
        Integer reviews,
        Integer age,
        List<String> tags,
        @JsonProperty("isNew") boolean isNew,
        String description,
        String image
) {
}
