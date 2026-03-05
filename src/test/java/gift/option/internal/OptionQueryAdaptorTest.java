package gift.option.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gift.category.Category;
import gift.global.NotFoundException;
import gift.option.Option;
import gift.option.OptionQueryPort;
import gift.product.Product;
import gift.product.ProductDto;
import gift.support.TestCategoryRepository;
import gift.support.TestOptionRepository;
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
@Import(OptionQueryAdaptor.class)
class OptionQueryAdaptorTest {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    @Autowired
    OptionQueryPort optionQueryPort;

    @Autowired
    TestCategoryRepository testCategoryRepo;

    @Autowired
    TestProductRepository testProductRepo;

    @Autowired
    TestOptionRepository testOptionRepo;

    @Autowired
    TransactionTemplate transactionTemplate;

    @AfterEach
    void tearDown() {
        testOptionRepo.deleteAllInBatch();
        testProductRepo.deleteAllInBatch();
        testCategoryRepo.deleteAllInBatch();
    }

    @Test
    @DisplayName("존재하는 옵션을 참조한다")
    void testGetReference() {
        // given
        Long categoryId = createCategory("교환권")
                .getId();
        Long productId = createProduct("상품A", 1000, categoryId)
                .getId();
        Option given = createOption("옵션A", 100, productId);

        // when
        Option result = transactionTemplate.execute(
                status -> optionQueryPort.getReference(given.getId())
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(given.getId());
        assertThat(result.getName()).isEqualTo(given.getName());
        assertThat(result.getQuantity()).isEqualTo(given.getQuantity());
        assertThat(result.getProduct().getId()).isEqualTo(given.getProduct().getId());
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
    private Product createProduct(String name, int price, Long categoryId) {
        return transactionTemplate.execute(status -> {
            Category category = testCategoryRepo.findById(categoryId)
                    .orElseThrow(AssertionError::new);

            return testProductRepo.save(Product.builder()
                    .name(name)
                    .price(price)
                    .imageUrl("http://img")
                    .category(category)
                    .build());
        });
    }

    @SuppressWarnings("SameParameterValue")
    private Option createOption(String name, int quantity, Long productId) {
        return transactionTemplate.execute(status -> {
            Product product = testProductRepo.findById(productId)
                    .orElseThrow(AssertionError::new);

            return testOptionRepo.save(Option.builder()
                    .name(name)
                    .quantity(quantity)
                    .product(product)
                    .build());
        });
    }

    @Test
    @DisplayName("존재하지 않는 옵션을 참조하면 NotFoundException 이 발생한다")
    void testGetReferenceNotFound() {
        // when + then
        assertThatThrownBy(() ->
                transactionTemplate.execute(
                        status -> optionQueryPort.getReference(NOT_EXISTING_ID)
                )
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("트랜잭션 없이 getReference 를 호출하면 IllegalTransactionStateException 이 발생한다")
    void testGetReferenceWithoutTransaction() {
        // given
        Long categoryId = createCategory("교환권")
                .getId();
        Long productId = createProduct("상품A", 1000, categoryId)
                .getId();
        Long optionId = createOption("옵션A", 100, productId)
                .getId();

        // when + then
        assertThatThrownBy(() -> optionQueryPort.getReference(optionId))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    @DisplayName("옵션에 연관된 상품 정보를 조회한다")
    void testGetAssociatedProduct() {
        // given
        Long categoryId = createCategory("교환권")
                .getId();
        Product givenProduct = createProduct("상품A", 1000, categoryId);
        Long optionId = createOption("옵션A", 100, givenProduct.getId())
                .getId();

        // when
        ProductDto result = optionQueryPort.getAssociatedProduct(optionId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(givenProduct.getId());
        assertThat(result.name()).isEqualTo(givenProduct.getName());
        assertThat(result.price()).isEqualTo(givenProduct.getPrice());
        assertThat(result.imageUrl()).isEqualTo(givenProduct.getImageUrl());
        assertThat(result.categoryId()).isEqualTo(givenProduct.getCategory().getId());
    }

    @Test
    @DisplayName("존재하지 않는 옵션의 연관 상품을 조회하면 NotFoundException 이 발생한다")
    void testGetAssociatedProductNotFound() {
        // when + then
        assertThatThrownBy(() -> optionQueryPort.getAssociatedProduct(NOT_EXISTING_ID))
                .isInstanceOf(NotFoundException.class);
    }
}
