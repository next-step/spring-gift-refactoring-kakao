package gift.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/setup-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ProductServiceTest {

    @Autowired
    ProductService productService;

    @Test
    @DisplayName("allowKakao=true이면 카카오 포함 상품명이 허용된다")
    void createWithKakaoAllowed() {
        Product product = productService.create("카카오 상품", 1000, "http://img.test/p.png", 1L, true);

        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isEqualTo("카카오 상품");
    }

    @Test
    @DisplayName("allowKakao=false이면 카카오 포함 상품명이 거부된다")
    void createWithKakaoNotAllowed() {
        assertThatThrownBy(() ->
            productService.create("카카오 상품", 1000, "http://img.test/p.png", 1L, false)
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
