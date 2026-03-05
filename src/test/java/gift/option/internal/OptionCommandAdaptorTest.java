package gift.option.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gift.category.Category;
import gift.global.NotFoundException;
import gift.option.Option;
import gift.option.OptionCommandPort;
import gift.product.Product;
import gift.support.TestCategoryRepository;
import gift.support.TestOptionRepository;
import gift.support.TestProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(OptionCommandAdaptor.class)
class OptionCommandAdaptorTest {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    @Autowired
    OptionCommandPort optionCommandPort;

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
    @DisplayName("옵션 수량을 차감한다")
    void testSubtractQuantity() {
        // given
        int quantity = 100;
        int subtractAmount = 30;

        Long categoryId = createCategory("교환권")
                .getId();
        Long productId = createProduct("상품A", 1000, categoryId)
                .getId();
        Long optionId = createOption("옵션A", quantity, productId)
                .getId();

        // when + then
        assertThatCode(() -> optionCommandPort.subtractQuantity(optionId, subtractAmount))
                .doesNotThrowAnyException();

        // then
        Option find = getOption(optionId);

        assertThat(find.getQuantity())
                .isEqualTo(quantity - subtractAmount);
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

    private Option getOption(Long id) {
        return testOptionRepo.findById(id)
                .orElseThrow(AssertionError::new);
    }

    @Test
    @DisplayName("차감할 수량이 재고보다 많으면 IllegalArgumentException 이 발생한다")
    void testSubtractQuantityInsufficientStock() {
        // given
        int quantity = 10;
        int subtractAmount = quantity + 1;

        Long categoryId = createCategory("교환권")
                .getId();
        Long productId = createProduct("상품A", 1000, categoryId)
                .getId();
        Long optionId = createOption("옵션A", quantity, productId)
                .getId();

        // when + then
        assertThatThrownBy(() -> optionCommandPort.subtractQuantity(optionId, subtractAmount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("존재하지 않는 옵션의 수량을 차감하면 NotFoundException 이 발생한다")
    void testSubtractQuantityNotFound() {
        // when + then
        assertThatThrownBy(() -> optionCommandPort.subtractQuantity(NOT_EXISTING_ID, 1))
                .isInstanceOf(NotFoundException.class);
    }
}
