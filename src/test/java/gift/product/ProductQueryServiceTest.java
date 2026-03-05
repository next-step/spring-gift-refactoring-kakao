package gift.product;

import gift.category.Category;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductQueryService productQueryService;

    private Product createProduct() {
        var category = new Category("전자기기", "#000", "https://example.com/img.jpg", "설명");
        return new Product("테스트상품", 10000, "https://example.com/img.jpg", category);
    }

    @Test
    @DisplayName("페이지네이션으로 상품 목록을 조회한다")
    void findAllPaged() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(createProduct()), pageable, 1);
        given(productRepository.findAll(pageable)).willReturn(page);

        Page<Product> result = productQueryService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("전체 상품 목록을 조회한다")
    void findAll() {
        given(productRepository.findAll()).willReturn(List.of(createProduct()));

        List<Product> result = productQueryService.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("ID로 상품을 조회한다")
    void findById() {
        given(productRepository.findById(1L)).willReturn(Optional.of(createProduct()));

        Product result = productQueryService.findById(1L);

        assertThat(result.getName()).isEqualTo("테스트상품");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
    void findById_NotFound() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productQueryService.findById(999L))
            .isInstanceOf(ProductException.class);
    }
}
