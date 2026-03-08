package gift;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.product.Product;
import gift.product.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AdminProductAcceptanceTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        databaseCleaner.clear();
    }

    Category createCategory(String name) {
        return categoryRepository.save(new Category(name, "#000000", "https://example.com/cat.jpg", ""));
    }

    @Test
    void 상품_목록_조회() throws Exception {
        Category category = createCategory("전자기기");
        productRepository.save(new Product("노트북", 1000000, "https://example.com/laptop.jpg", category));
        productRepository.save(new Product("키보드", 50000, "https://example.com/keyboard.jpg", category));

        mockMvc.perform(get("/admin/products"))
            .andExpect(status().isOk())
            .andExpect(view().name("product/list"))
            .andExpect(model().attribute("products", hasSize(2)));
    }

    @Test
    void 상품_생성_성공() throws Exception {
        Category category = createCategory("전자기기");

        mockMvc.perform(post("/admin/products")
                .param("name", "노트북")
                .param("price", "1000000")
                .param("imageUrl", "https://example.com/laptop.jpg")
                .param("categoryId", category.getId().toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/products"));
    }

    @Test
    void 상품_생성_실패_이름_검증() throws Exception {
        Category category = createCategory("전자기기");

        mockMvc.perform(post("/admin/products")
                .param("name", "")
                .param("price", "1000")
                .param("imageUrl", "https://example.com/img.jpg")
                .param("categoryId", category.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(view().name("product/new"))
            .andExpect(model().attributeExists("errors"));
    }

    @Test
    void 상품_수정() throws Exception {
        Category category = createCategory("전자기기");
        Product product = productRepository.save(
            new Product("노트북", 1000000, "https://example.com/laptop.jpg", category));

        mockMvc.perform(post("/admin/products/{id}/edit", product.getId())
                .param("name", "게이밍노트북")
                .param("price", "2000000")
                .param("imageUrl", "https://example.com/gaming.jpg")
                .param("categoryId", category.getId().toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/products"));
    }

    @Test
    void 상품_삭제() throws Exception {
        Category category = createCategory("전자기기");
        Product product = productRepository.save(
            new Product("노트북", 1000000, "https://example.com/laptop.jpg", category));

        mockMvc.perform(post("/admin/products/{id}/delete", product.getId()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/products"));
    }
}
