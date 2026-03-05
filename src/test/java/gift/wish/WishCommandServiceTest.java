package gift.wish;

import gift.category.Category;
import gift.product.Product;
import gift.product.ProductRepository;
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
class WishCommandServiceTest {

    @Mock
    private WishRepository wishRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private WishCommandService wishCommandService;

    private Product createProduct() {
        var category = new Category("전자기기", "#000", "https://example.com/img.jpg", "설명");
        return new Product("테스트상품", 10000, "https://example.com/img.jpg", category);
    }

    @Test
    @DisplayName("위시를 추가한다")
    void addWish() {
        var product = createProduct();
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(wishRepository.save(any(Wish.class))).willAnswer(inv -> inv.getArgument(0));

        Wish result = wishCommandService.addWish(1L, 1L);

        assertThat(result.getMemberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 상품을 위시에 추가하면 예외가 발생한다")
    void addWish_ProductNotFound() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> wishCommandService.addWish(1L, 999L))
            .isInstanceOf(WishException.class);
    }

    @Test
    @DisplayName("위시를 삭제한다")
    void delete() {
        var wish = new Wish(1L, createProduct());

        wishCommandService.delete(wish);

        then(wishRepository).should().delete(wish);
    }
}
