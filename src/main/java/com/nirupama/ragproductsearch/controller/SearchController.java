package com.nirupama.ragproductsearch.controller;

import com.nirupama.ragproductsearch.dto.SearchResult;
import com.nirupama.ragproductsearch.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search/raw")
    public List<SearchResult> searchRaw(@RequestParam String query) {
        return searchService.searchProducts(query);
    }
}