package gift.option;

import gift.category.Category;
import gift.product.Product;
import gift.product.ProductException;
import gift.product.ProductRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OptionCommandServiceTest {

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OptionCommandService optionCommandService;

    private Product createProductWithId(Long id) {
        var category = new Category("전자기기", "#000", "https://example.com/img.jpg", "설명");
        var product = new Product("테스트상품", 10000, "https://example.com/img.jpg", category);
        try {
            Field idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(product, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return product;
    }

    @Test
    @DisplayName("옵션을 생성한다")
    void createOption() {
        var product = createProductWithId(1L);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(optionRepository.existsByProductIdAndName(1L, "새옵션")).willReturn(false);
        given(optionRepository.save(any(Option.class))).willAnswer(inv -> inv.getArgument(0));

        Option result = optionCommandService.createOption(1L, "새옵션", 50);

        assertThat(result.getName()).isEqualTo("새옵션");
        assertThat(result.getQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("존재하지 않는 상품에 옵션을 생성하면 예외가 발생한다")
    void createOption_ProductNotFound() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> optionCommandService.createOption(999L, "옵션", 10))
            .isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("중복 옵션명으로 생성하면 예외가 발생한다")
    void createOption_DuplicateName() {
        var product = createProductWithId(1L);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(optionRepository.existsByProductIdAndName(1L, "기존옵션")).willReturn(true);

        assertThatThrownBy(() -> optionCommandService.createOption(1L, "기존옵션", 10))
            .isInstanceOf(OptionException.class);
    }

    @Test
    @DisplayName("옵션이 2개 이상일 때 삭제한다")
    void deleteOption() {
        var product = createProductWithId(1L);
        var option1 = new Option(product, "옵션A", 10);
        var option2 = new Option(product, "옵션B", 20);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(optionRepository.findByProductId(1L)).willReturn(List.of(option1, option2));
        given(optionRepository.findById(1L)).willReturn(Optional.of(option1));

        optionCommandService.deleteOption(1L, 1L);

        then(optionRepository).should().delete(option1);
    }

    @Test
    @DisplayName("옵션이 1개뿐이면 삭제 시 예외가 발생한다")
    void deleteOption_LastOption() {
        var product = createProductWithId(1L);
        var option = new Option(product, "옵션A", 10);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(optionRepository.findByProductId(1L)).willReturn(List.of(option));

        assertThatThrownBy(() -> optionCommandService.deleteOption(1L, 1L))
            .isInstanceOf(OptionException.class);
    }

    @Test
    @DisplayName("존재하지 않는 옵션을 삭제하면 예외가 발생한다")
    void deleteOption_OptionNotFound() {
        var product = createProductWithId(1L);
        var option1 = new Option(product, "옵션A", 10);
        var option2 = new Option(product, "옵션B", 20);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(optionRepository.findByProductId(1L)).willReturn(List.of(option1, option2));
        given(optionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> optionCommandService.deleteOption(1L, 999L))
            .isInstanceOf(OptionException.class);
    }
}
