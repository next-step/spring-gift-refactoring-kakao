package gift.wish;

import gift.ForbiddenException;
import gift.category.Category;
import gift.product.Product;
import gift.product.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WishServiceTest {

    @Mock
    private WishRepository wishRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private WishService wishService;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("존재하는 상품이면 위시를 생성한다")
        void success() {
            Product product = createProduct();
            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(wishRepository.save(any(Wish.class))).willAnswer(inv -> inv.getArgument(0));

            Wish wish = wishService.create(100L, 1L);

            assertThat(wish.getMemberId()).isEqualTo(100L);
            assertThat(wish.getProduct()).isEqualTo(product);
        }

        @Test
        @DisplayName("존재하지 않는 상품이면 예외가 발생한다")
        void productNotFound() {
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> wishService.create(100L, 999L))
                .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove {

        @Test
        @DisplayName("본인의 위시를 삭제할 수 있다")
        void ownerCanDelete() {
            Wish wish = new Wish(100L, createProduct());
            given(wishRepository.findById(1L)).willReturn(Optional.of(wish));

            wishService.remove(100L, 1L);

            verify(wishRepository).delete(wish);
        }

        @Test
        @DisplayName("타인의 위시를 삭제하면 ForbiddenException이 발생한다")
        void otherMemberCannotDelete() {
            Wish wish = new Wish(100L, createProduct());
            given(wishRepository.findById(1L)).willReturn(Optional.of(wish));

            assertThatThrownBy(() -> wishService.remove(999L, 1L))
                .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("존재하지 않는 위시를 삭제하면 예외가 발생한다")
        void wishNotFound() {
            given(wishRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> wishService.remove(100L, 999L))
                .isInstanceOf(NoSuchElementException.class);
        }
    }

    private Product createProduct() {
        Category category = new Category("카테고리", "#000", "img.png", "설명");
        return new Product("상품", 1000, "img.png", category);
    }
}
