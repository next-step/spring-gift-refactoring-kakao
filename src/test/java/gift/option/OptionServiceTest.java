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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OptionServiceTest {

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OptionService optionService;

    private Product product;

    @BeforeEach
    void setUp() throws Exception {
        Category category = new Category("카테고리", "#000", "img.png", "설명");
        product = new Product("상품", 1000, "img.png", category);
        setId(product, 1L);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("같은 상품에 중복된 옵션명이 있으면 예외가 발생한다")
        void duplicateName() {
            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(optionRepository.existsByProductIdAndName(1L, "기존옵션")).willReturn(true);

            assertThatThrownBy(() -> optionService.create(1L, new OptionRequest("기존옵션", 10)))
                .isInstanceOf(IllegalArgumentException.class);
            verify(optionRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 상품이면 예외가 발생한다")
        void productNotFound() {
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> optionService.create(999L, new OptionRequest("옵션", 10)))
                .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("옵션이 1개뿐이면 삭제할 수 없다")
        void cannotDeleteLastOption() {
            Option singleOption = new Option(product, "유일옵션", 10);
            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(optionRepository.findByProductId(1L)).willReturn(List.of(singleOption));

            assertThatThrownBy(() -> optionService.delete(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
            verify(optionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("옵션이 2개 이상이면 삭제할 수 있다")
        void deleteWhenMultipleOptions() throws Exception {
            Option option1 = new Option(product, "옵션1", 10);
            Option option2 = new Option(product, "옵션2", 20);
            setId(option1, 10L);
            setId(option2, 11L);
            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(optionRepository.findByProductId(1L)).willReturn(List.of(option1, option2));
            given(optionRepository.findById(10L)).willReturn(Optional.of(option1));

            optionService.delete(1L, 10L);

            verify(optionRepository).delete(option1);
        }
    }
}
