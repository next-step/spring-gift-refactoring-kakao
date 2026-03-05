package gift.option;

import gift.category.Category;
import gift.product.Product;
import gift.product.ProductException;
import gift.product.ProductRepository;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OptionQueryServiceTest {

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OptionQueryService optionQueryService;

    private Product createProduct() {
        var category = new Category("전자기기", "#000", "https://example.com/img.jpg", "설명");
        return new Product("테스트상품", 10000, "https://example.com/img.jpg", category);
    }

    @Test
    @DisplayName("상품의 옵션 목록을 조회한다")
    void findByProductId() {
        var product = createProduct();
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(optionRepository.findByProductId(1L)).willReturn(List.of(
            new Option(product, "옵션A", 10),
            new Option(product, "옵션B", 20)
        ));

        List<Option> result = optionQueryService.findByProductId(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("존재하지 않는 상품의 옵션을 조회하면 예외가 발생한다")
    void findByProductId_ProductNotFound() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> optionQueryService.findByProductId(999L))
            .isInstanceOf(ProductException.class);
    }
}
