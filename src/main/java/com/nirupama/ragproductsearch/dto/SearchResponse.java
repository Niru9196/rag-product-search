package com.nirupama.ragproductsearch.dto;

import java.util.List;

public record SearchResponse(String answer, List<SearchResult> matchedProducts) {
}