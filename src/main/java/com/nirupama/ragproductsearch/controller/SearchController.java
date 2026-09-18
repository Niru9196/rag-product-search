package com.nirupama.ragproductsearch.controller;

import com.nirupama.ragproductsearch.dto.SearchRequest;
import com.nirupama.ragproductsearch.dto.SearchResponse;
import com.nirupama.ragproductsearch.dto.SearchResult;
import com.nirupama.ragproductsearch.service.SearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search/raw")
    public List<SearchResult> searchRaw(@RequestParam String query) {
        return searchService.searchProductsRaw(query);
    }

    @PostMapping("/api/search")
    public SearchResponse search(@RequestBody SearchRequest request) {
        return searchService.searchWithSynthesis(request.query());
    }
}