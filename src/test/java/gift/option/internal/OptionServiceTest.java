package gift.option.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import gift.global.NotFoundException;
import gift.option.Option;
import gift.product.Product;
import gift.product.ProductQueryPort;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OptionServiceTest {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    @InjectMocks
    OptionService optionService;

    @Mock
    ProductQueryPort productQueryPort;

    @Mock
    OptionRepository optionRepo;

    @Test
    @DisplayName("옵션 목록을 조회한다 — validateExists + findAllByProductId 호출 + 응답 매핑")
    void testGetOptions() {
        // given
        Long productId = 10L;
        Product product = createProduct(productId, "상품", 1000, "https://img.png");
        Option option1 = createOption(1L, product, "옵션A", 100);
        Option option2 = createOption(2L, product, "옵션B", 200);

        given(optionRepo.findAllByProductId(productId))
                .willReturn(List.of(option1, option2));

        // when
        List<OptionResponse> responses = optionService.getOptions(productId);

        // then
        then(productQueryPort).should()
                .validateExists(productId);
        then(optionRepo).should()
                .findAllByProductId(productId);

        assertThat(responses).hasSize(2);

        OptionResponse first = responses.getFirst();
        assertThat(first.id()).isEqualTo(option1.getId());
        assertThat(first.name()).isEqualTo(option1.getName());
        assertThat(first.quantity()).isEqualTo(option1.getQuantity());

        OptionResponse second = responses.get(1);
        assertThat(second.id()).isEqualTo(option2.getId());
        assertThat(second.name()).isEqualTo(option2.getName());
        assertThat(second.quantity()).isEqualTo(option2.getQuantity());
    }

    @SuppressWarnings("SameParameterValue")
    private static Product createProduct(Long id, String name, int price, String imageUrl) {
        Product product = Product.builder()
                .name(name)
                .price(price)
                .imageUrl(imageUrl)
                .build();

        setId(product, Product.class, id);
        return product;
    }

    private static Option createOption(Long id, Product product, String name, int quantity) {
        Option option = Option.builder()
                .product(product)
                .name(name)
                .quantity(quantity)
                .build();

        setId(option, Option.class, id);
        return option;
    }

    private static <T> void setId(T entity, Class<T> clazz, Long id) {
        try {
            Field idField = clazz.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    @DisplayName("옵션 목록 조회 시 상품이 없으면 NotFoundException 이 전파된다")
    void testGetOptionsProductNotFound() {
        // given
        willThrow(NotFoundException.productNotFound())
                .given(productQueryPort).validateExists(NOT_EXISTING_ID);

        // when + then
        assertThatThrownBy(() -> optionService.getOptions(NOT_EXISTING_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("옵션을 생성한다 — getReference 호출 + 중복 없음 → save + 응답 매핑")
    void testCreateOption() {
        // given
        Long productId = 10L;
        Product product = createProduct(productId, "상품", 1000, "https://img.png");
        Option savedOption = createOption(1L, product, "새옵션", 100);

        given(productQueryPort.getReference(productId))
                .willReturn(product);
        given(optionRepo.existsByProductIdAndName(productId, "새옵션"))
                .willReturn(false);
        given(optionRepo.save(any(Option.class)))
                .willReturn(savedOption);

        // when
        OptionResponse response = optionService.createOption(productId,
                new OptionRequest("새옵션", 100));

        // then
        then(productQueryPort).should()
                .getReference(productId);
        then(optionRepo).should()
                .save(any(Option.class));

        assertThat(response.id()).isEqualTo(savedOption.getId());
        assertThat(response.name()).isEqualTo("새옵션");
        assertThat(response.quantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("옵션 생성 시 상품이 없으면 NotFoundException 이 전파된다")
    void testCreateOptionProductNotFound() {
        // given
        given(productQueryPort.getReference(NOT_EXISTING_ID))
                .willThrow(NotFoundException.productNotFound());

        // when + then
        assertThatThrownBy(
                () -> optionService.createOption(NOT_EXISTING_ID, new OptionRequest("옵션", 100)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("옵션 생성 시 이름이 중복되면 DuplicateOptionNameException 이 발생한다")
    void testCreateOptionDuplicateName() {
        // given
        Long productId = 10L;
        Product product = createProduct(productId, "상품", 1000, "https://img.png");

        given(productQueryPort.getReference(productId))
                .willReturn(product);
        given(optionRepo.existsByProductIdAndName(productId, "기존옵션"))
                .willReturn(true);

        // when + then
        assertThatThrownBy(
                () -> optionService.createOption(productId, new OptionRequest("기존옵션", 100)))
                .isInstanceOf(DuplicateOptionNameException.class);
    }

    @Test
    @DisplayName("옵션을 수정한다 — validateExists + 중복 체크(자기 제외) + 조회 + Entity 상태 변경 + 응답 매핑")
    void testUpdateOption() {
        // given
        Long productId = 10L;
        Long optionId = 1L;
        Product product = createProduct(productId, "상품", 1000, "https://img.png");
        Option option = createOption(optionId, product, "TALL", 100);

        given(optionRepo.existsByProductIdAndNameAndIdNot(productId, "GRANDE", optionId))
                .willReturn(false);
        given(optionRepo.findByIdAndProductId(optionId, productId))
                .willReturn(Optional.of(option));

        // when
        OptionResponse response = optionService.updateOption(
                productId, optionId, new OptionRequest("GRANDE", 200)
        );

        // then
        then(productQueryPort).should()
                .validateExists(productId);
        then(optionRepo).should()
                .existsByProductIdAndNameAndIdNot(productId, "GRANDE", optionId);

        assertThat(response.id()).isEqualTo(optionId);
        assertThat(response.name()).isEqualTo("GRANDE");
        assertThat(response.quantity()).isEqualTo(200);

        assertThat(option.getName()).isEqualTo("GRANDE");
        assertThat(option.getQuantity()).isEqualTo(200);
    }

    @Test
    @DisplayName("옵션 수정 시 상품이 없으면 NotFoundException 이 전파된다")
    void testUpdateOptionProductNotFound() {
        // given
        willThrow(NotFoundException.productNotFound())
                .given(productQueryPort).validateExists(NOT_EXISTING_ID);

        // when + then
        assertThatThrownBy(() -> optionService.updateOption(
                NOT_EXISTING_ID, 1L, new OptionRequest("옵션", 100)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("옵션 수정 시 해당 옵션이 없으면 NotFoundException 이 발생한다")
    void testUpdateOptionNotFound() {
        // given
        Long productId = 10L;

        given(optionRepo.existsByProductIdAndNameAndIdNot(productId, "옵션", NOT_EXISTING_ID))
                .willReturn(false);
        given(optionRepo.findByIdAndProductId(NOT_EXISTING_ID, productId))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> optionService.updateOption(
                productId, NOT_EXISTING_ID, new OptionRequest("옵션", 100)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("옵션 수정 시 다른 옵션과 이름이 중복되면 DuplicateOptionNameException 이 발생한다")
    void testUpdateOptionDuplicateName() {
        // given
        Long productId = 10L;
        Long optionId = 1L;

        given(optionRepo.existsByProductIdAndNameAndIdNot(productId, "기존옵션", optionId))
                .willReturn(true);

        // when + then
        assertThatThrownBy(() -> optionService.updateOption(
                productId, optionId, new OptionRequest("기존옵션", 100)))
                .isInstanceOf(DuplicateOptionNameException.class);
    }

    @Test
    @DisplayName("옵션을 삭제한다 — validateExists + countByProductId > 1 + findByIdAndProductId + delete")
    void testDeleteOption() {
        // given
        Long productId = 10L;
        Long optionId = 1L;
        Product product = createProduct(productId, "상품", 1000, "https://img.png");
        Option option = createOption(optionId, product, "옵션A", 100);

        given(optionRepo.countByProductId(productId))
                .willReturn(2L);
        given(optionRepo.findByIdAndProductId(optionId, productId))
                .willReturn(Optional.of(option));

        // when
        optionService.deleteOption(productId, optionId);

        // then
        then(productQueryPort).should().validateExists(productId);
        then(optionRepo).should().delete(option);
    }

    // -- fixtures --

    @Test
    @DisplayName("옵션 삭제 시 상품이 없으면 NotFoundException 이 전파된다")
    void testDeleteOptionProductNotFound() {
        // given
        willThrow(NotFoundException.productNotFound())
                .given(productQueryPort).validateExists(NOT_EXISTING_ID);

        // when + then
        assertThatThrownBy(() -> optionService.deleteOption(NOT_EXISTING_ID, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("옵션이 1개인 상품은 옵션을 삭제할 수 없다 — FailedToDeleteOptionException")
    void testDeleteOptionInsufficientRemaining() {
        // given
        Long productId = 10L;
        Long optionId = 1L;

        given(optionRepo.countByProductId(productId))
                .willReturn(1L);

        // when + then
        assertThatThrownBy(() -> optionService.deleteOption(productId, optionId))
                .isInstanceOf(FailedToDeleteOptionException.class);
    }

    @Test
    @DisplayName("옵션 삭제 시 해당 옵션이 없으면 NotFoundException 이 발생한다")
    void testDeleteOptionNotFound() {
        // given
        Long productId = 10L;
        Long optionId = NOT_EXISTING_ID;

        given(optionRepo.countByProductId(productId))
                .willReturn(2L);
        given(optionRepo.findByIdAndProductId(optionId, productId))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> optionService.deleteOption(productId, optionId))
                .isInstanceOf(NotFoundException.class);
    }
}
