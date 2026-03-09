package gift;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.product.ProductRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenInViewTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    OptionRepository optionRepository;

    @Test
    void LAZY_컬렉션_트랜잭션_밖_접근_시_예외() {
        // given — 데이터 저장 (각 save는 자체 트랜잭션)
        Category category = categoryRepository.save(new Category("테스트", "#000000", "http://test.jpg", ""));
        Product product = productRepository.save(new Product("테스트상품", 1000, "http://test.jpg", category));
        optionRepository.save(new Option(product, "옵션A", 10));

        // when — 트랜잭션 밖에서 상품을 다시 조회
        Product loaded = productRepository.findById(product.getId()).get();

        // then — LAZY 컬렉션 접근 시 LazyInitializationException 발생
        assertThrows(LazyInitializationException.class,
            () -> loaded.getOptions().size());
    }
}
