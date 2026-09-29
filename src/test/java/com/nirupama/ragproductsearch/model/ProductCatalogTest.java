package com.nirupama.ragproductsearch.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCatalogTest {

    private static List<Product> products;

    @BeforeAll
    static void loadCatalog() throws Exception {
        try (InputStream in = new ClassPathResource("products.json").getInputStream()) {
            products = Arrays.asList(new ObjectMapper().readValue(in, Product[].class));
        }
    }

    @Test
    void catalogHas36ProductsWithUniqueIds1To36() {
        assertThat(products).hasSize(36);
        assertThat(products).extracting(Product::id)
                .containsExactlyInAnyOrderElementsOf(IntStream.rangeClosed(1, 36).boxed().toList());
    }

    @Test
    void everyProductPointsToAnExistingImage() {
        for (Product p : products) {
            assertThat(p.image()).isEqualTo("/images/product_" + p.id() + ".png");
            assertThat(new ClassPathResource("static" + p.image()).exists())
                    .as("image file for product %d", p.id())
                    .isTrue();
        }
    }

    @Test
    void fieldsAreMappedIncludingIsNew() {
        Product first = byId(1);
        Product second = byId(2);

        assertThat(first.isNew()).isFalse();
        assertThat(second.isNew()).isTrue();
        assertThat(first.name()).isEqualTo("Striped Flutter Sleeve Peplum Blouse");
        assertThat(first.was()).isEqualTo(80.5);
        assertThat(products).allSatisfy(p -> {
            assertThat(p.colors()).isNotEmpty();
            assertThat(p.tags()).isNotEmpty();
            assertThat(p.category()).isIn("women", "men", "kid");
        });
    }

    private static Product byId(int id) {
        return products.stream().filter(p -> p.id() == id).findFirst().orElseThrow();
    }
}
