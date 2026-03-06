package gift.service;

import gift.model.Category;
import gift.model.Option;
import gift.model.Product;
import gift.repository.OptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class OptionServiceTest {

    @Mock
    private OptionRepository optionRepository;
    @Mock
    private ProductService productService;

    @InjectMocks
    private OptionService optionService;

    private Product product;
    private Option option;

    @BeforeEach
    void setUp() {
        var category = new Category("카테고리", "#000000", "image.png", "설명");
        product = new Product("상품", 1000, "image.png", category);
        ReflectionTestUtils.setField(product, "id", 1L);
        option = new Option(product, "옵션A", 10);
        ReflectionTestUtils.setField(option, "id", 1L);
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("성공 - 이름과 수량이 변경된다")
        void success() {
            given(productService.findById(1L)).willReturn(product);
            given(optionRepository.findById(1L)).willReturn(Optional.of(option));
            given(optionRepository.existsByProductIdAndNameAndIdNot(any(), any(), any())).willReturn(false);
            given(optionRepository.save(any(Option.class))).willAnswer(invocation -> invocation.getArgument(0));

            Option result = optionService.update(1L, 1L, "변경된옵션", 20);

            assertThat(result.getName()).isEqualTo("변경된옵션");
            assertThat(result.getQuantity()).isEqualTo(20);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 옵션이면 예외가 발생한다")
        void failsWhenOptionNotFound() {
            given(productService.findById(1L)).willReturn(product);
            given(optionRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> optionService.update(1L, 999L, "변경된옵션", 20))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Option not found");
        }

        @Test
        @DisplayName("실패 - 같은 상품 내 중복 이름이면 예외가 발생한다")
        void failsWhenDuplicateName() {
            given(productService.findById(1L)).willReturn(product);
            given(optionRepository.findById(1L)).willReturn(Optional.of(option));
            given(optionRepository.existsByProductIdAndNameAndIdNot(any(), any(), any())).willReturn(true);

            assertThatThrownBy(() -> optionService.update(1L, 1L, "중복옵션", 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Option name already exists.");
        }
    }
}
