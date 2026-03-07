package gift.product.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gift.category.Category;
import gift.global.NotFoundException;
import gift.product.Product;
import gift.product.ProductQueryPort;
import gift.support.TestCategoryRepository;
import gift.support.TestProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(ProductQueryAdaptor.class)
class ProductQueryAdaptorTest {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    @Autowired
    ProductQueryPort productQueryPort;

    @Autowired
    TestCategoryRepository testCategoryRepo;

    @Autowired
    TestProductRepository testProductRepo;

    @Autowired
    TransactionTemplate transactionTemplate;

    @AfterEach
    void tearDown() {
        testProductRepo.deleteAllInBatch();
        testCategoryRepo.deleteAllInBatch();
    }

    @Test
    @DisplayName("존재하는 상품을 참조한다")
    void testGetReference() {
        // given
        Long categoryId = createCategory("교환권")
                .getId();
        Product given = createProduct("상품A", 1000, "http://img", categoryId);

        // when
        Product result = transactionTemplate.execute(
                status -> productQueryPort.getReference(given.getId())
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(given.getId());
        assertThat(result.getName()).isEqualTo(given.getName());
        assertThat(result.getPrice()).isEqualTo(given.getPrice());
        assertThat(result.getImageUrl()).isEqualTo(given.getImageUrl());
        assertThat(result.getCategory().getId()).isEqualTo(given.getCategory().getId());
    }

    @SuppressWarnings("SameParameterValue")
    private Category createCategory(String name) {
        return testCategoryRepo.save(Category.builder()
                .name(name)
                .color("#000000")
                .imageUrl("http://img")
                .description("설명")
                .build());
    }

    @SuppressWarnings("SameParameterValue")
    private Product createProduct(
            String name, int price,
            String imageUrl, Long categoryId
    ) {
        return transactionTemplate.execute(status -> {
            Category category = testCategoryRepo.findById(categoryId)
                    .orElseThrow(AssertionError::new);

            return testProductRepo.save(Product.builder()
                    .name(name)
                    .price(price)
                    .imageUrl(imageUrl)
                    .category(category)
                    .build());
        });
    }

    @Test
    @DisplayName("존재하지 않는 상품을 참조하면 NotFoundException 이 발생한다")
    void testGetReferenceNotFound() {
        // when + then
        assertThatThrownBy(() ->
                transactionTemplate.execute(
                        status -> productQueryPort.getReference(NOT_EXISTING_ID)
                )
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("트랜잭션 없이 getReference 를 호출하면 IllegalTransactionStateException 이 발생한다")
    void testGetReferenceWithoutTransaction() {
        // given
        Long categoryId = createCategory("교환권")
                .getId();
        Long productId = createProduct("상품A", 1000, "http://img", categoryId)
                .getId();

        // when + then
        assertThatThrownBy(() -> productQueryPort.getReference(productId))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    @DisplayName("존재하는 상품의 존재를 검증한다")
    void testValidateExists() {
        // given
        Long categoryId = createCategory("교환권")
                .getId();
        Long productId = createProduct("상품A", 1000, "http://img", categoryId)
                .getId();

        // when + then
        assertThatCode(() -> productQueryPort.validateExists(productId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("존재하지 않는 상품의 존재를 검증하면 NotFoundException 이 발생한다")
    void testValidateExistsNotFound() {
        // when + then
        assertThatThrownBy(() -> productQueryPort.validateExists(NOT_EXISTING_ID))
                .isInstanceOf(NotFoundException.class);
    }
}
