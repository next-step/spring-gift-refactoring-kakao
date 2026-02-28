package gift.wish;

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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
class WishServiceTest {

    @Mock
    private WishRepository wishRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private WishService wishService;

    private Product product;

    @BeforeEach
    void setUp() throws Exception {
        var category = new Category("교환권", "#ffffff", "img.png", "");
        setId(category, 1L);

        product = new Product("스타벅스 아메리카노", 5000, "img.png", category);
        setId(product, 1L);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Nested
    @DisplayName("getWishes")
    class GetWishes {

        @Test
        @DisplayName("회원의 위시 목록을 페이징 조회한다")
        void returnsPagedWishes() throws Exception {
            var wish = new Wish(1L, product);
            setId(wish, 1L);
            var pageable = PageRequest.of(0, 10);

            given(wishRepository.findByMemberId(1L, pageable))
                .willReturn(new PageImpl<>(List.of(wish)));

            var result = wishService.getWishes(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).productId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("addWish")
    class AddWish {

        @Test
        @DisplayName("신규 위시를 생성하면 created=true를 반환한다")
        void createsNewWish() throws Exception {
            var request = new WishRequest(1L);
            var saved = new Wish(1L, product);
            setId(saved, 1L);

            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(wishRepository.findByMemberIdAndProductId(1L, 1L)).willReturn(Optional.empty());
            given(wishRepository.save(any(Wish.class))).willReturn(saved);

            var result = wishService.addWish(1L, request);

            assertThat(result.created()).isTrue();
            assertThat(result.wish().getProduct().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("이미 존재하는 위시면 created=false를 반환한다")
        void returnsExistingWish() throws Exception {
            var request = new WishRequest(1L);
            var existing = new Wish(1L, product);
            setId(existing, 1L);

            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(wishRepository.findByMemberIdAndProductId(1L, 1L)).willReturn(Optional.of(existing));

            var result = wishService.addWish(1L, request);

            assertThat(result.created()).isFalse();
            assertThat(result.wish().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 예외를 던진다")
        void throwsWhenProductNotFound() {
            var request = new WishRequest(999L);

            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> wishService.addWish(1L, request))
                .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("removeWish")
    class RemoveWish {

        @Test
        @DisplayName("본인의 위시를 삭제한다")
        void removesSuccessfully() throws Exception {
            var wish = new Wish(1L, product);
            setId(wish, 1L);

            given(wishRepository.findById(1L)).willReturn(Optional.of(wish));

            wishService.removeWish(1L, 1L);

            then(wishRepository).should().delete(wish);
        }

        @Test
        @DisplayName("위시가 존재하지 않으면 예외를 던진다")
        void throwsWhenWishNotFound() {
            given(wishRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> wishService.removeWish(1L, 999L))
                .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("본인의 위시가 아니면 예외를 던진다")
        void throwsWhenNotOwner() throws Exception {
            var wish = new Wish(2L, product);
            setId(wish, 1L);

            given(wishRepository.findById(1L)).willReturn(Optional.of(wish));

            assertThatThrownBy(() -> wishService.removeWish(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
