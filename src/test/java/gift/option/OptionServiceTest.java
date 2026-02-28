package gift.option;

import gift.category.Category;
import gift.product.Product;
import gift.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OptionServiceTest {

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OptionService optionService;

    private Product product;
    private Option option;

    @BeforeEach
    void setUp() throws Exception {
        var category = new Category("교환권", "#ffffff", "img.png", "");
        setId(category, 1L);

        product = new Product("스타벅스 아메리카노", 5000, "img.png", category);
        setId(product, 1L);

        option = new Option(product, "Tall", 100);
        setId(option, 1L);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Nested
    @DisplayName("getOptions")
    class GetOptions {

        @Test
        @DisplayName("상품의 옵션 목록을 조회한다")
        void returnsOptions() {
            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(optionRepository.findByProductId(1L)).willReturn(List.of(option));

            List<OptionResponse> result = optionService.getOptions(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Tall");
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 예외를 던진다")
        void throwsWhenProductNotFound() {
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> optionService.getOptions(999L))
                .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("createOption")
    class CreateOption {

        @Test
        @DisplayName("정상적으로 옵션을 생성한다")
        void createsSuccessfully() throws Exception {
            var request = new OptionRequest("Grande", 50);
            var saved = new Option(product, "Grande", 50);
            setId(saved, 2L);

            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(optionRepository.existsByProductIdAndName(1L, "Grande")).willReturn(false);
            given(optionRepository.save(any(Option.class))).willReturn(saved);

            Option result = optionService.createOption(1L, request);

            assertThat(result.getName()).isEqualTo("Grande");
            assertThat(result.getQuantity()).isEqualTo(50);
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 예외를 던진다")
        void throwsWhenProductNotFound() {
            var request = new OptionRequest("Grande", 50);

            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> optionService.createOption(999L, request))
                .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("옵션명이 중복이면 예외를 던진다")
        void throwsWhenNameDuplicated() {
            var request = new OptionRequest("Tall", 50);

            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(optionRepository.existsByProductIdAndName(1L, "Tall")).willReturn(true);

            assertThatThrownBy(() -> optionService.createOption(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("옵션명에 허용되지 않는 특수문자가 있으면 예외를 던진다")
        void throwsWhenNameInvalid() {
            var request = new OptionRequest("옵션@!#", 50);

            assertThatThrownBy(() -> optionService.createOption(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("deleteOption")
    class DeleteOption {

        @Test
        @DisplayName("정상적으로 옵션을 삭제한다")
        void deletesSuccessfully() throws Exception {
            var option2 = new Option(product, "Grande", 50);
            setId(option2, 2L);

            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(optionRepository.findByProductId(1L)).willReturn(List.of(option, option2));
            given(optionRepository.findById(1L)).willReturn(Optional.of(option));

            optionService.deleteOption(1L, 1L);

            then(optionRepository).should().delete(option);
        }

        @Test
        @DisplayName("옵션이 1개뿐이면 삭제할 수 없다")
        void throwsWhenOnlyOneOption() {
            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(optionRepository.findByProductId(1L)).willReturn(List.of(option));

            assertThatThrownBy(() -> optionService.deleteOption(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("옵션이 존재하지 않으면 예외를 던진다")
        void throwsWhenOptionNotFound() throws Exception {
            var option2 = new Option(product, "Grande", 50);
            setId(option2, 2L);

            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(optionRepository.findByProductId(1L)).willReturn(List.of(option, option2));
            given(optionRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> optionService.deleteOption(1L, 999L))
                .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 예외를 던진다")
        void throwsWhenProductNotFound() {
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> optionService.deleteOption(999L, 1L))
                .isInstanceOf(NoSuchElementException.class);
        }
    }
}
