package gift.wish;

import gift.TestFixtures;
import gift.product.Product;
import gift.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
    void setUp() {
        product = TestFixtures.product();
    }

    @Test
    void findByMemberId_returnsPagedWishes() {
        var wish = TestFixtures.wish(1L, 1L, product);
        var pageable = PageRequest.of(0, 10);
        given(wishRepository.findByMemberId(1L, pageable)).willReturn(new PageImpl<>(List.of(wish)));

        var result = wishService.findByMemberId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void add_newWish_returnsCreatedTrue() {
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(wishRepository.findByMemberIdAndProductId(1L, 1L)).willReturn(Optional.empty());
        given(wishRepository.save(any(Wish.class))).willAnswer(inv -> inv.getArgument(0));

        var result = wishService.add(1L, 1L);

        assertThat(result.created()).isTrue();
        then(wishRepository).should().save(any(Wish.class));
    }

    @Test
    void add_duplicateWish_returnsCreatedFalse() {
        var existing = TestFixtures.wish(1L, 1L, product);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(wishRepository.findByMemberIdAndProductId(1L, 1L)).willReturn(Optional.of(existing));

        var result = wishService.add(1L, 1L);

        assertThat(result.created()).isFalse();
        assertThat(result.wish()).isEqualTo(existing);
    }

    @Test
    void add_productNotFound_throws() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> wishService.add(1L, 99L))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void remove_happyPath_deletes() {
        var wish = TestFixtures.wish(1L, 1L, product);
        given(wishRepository.findById(1L)).willReturn(Optional.of(wish));

        wishService.remove(1L, 1L);

        then(wishRepository).should().delete(wish);
    }

    @Test
    void remove_wishNotFound_throws() {
        given(wishRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> wishService.remove(1L, 99L))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void remove_notOwner_throwsIllegalState() {
        var wish = TestFixtures.wish(1L, 2L, product);
        given(wishRepository.findById(1L)).willReturn(Optional.of(wish));

        assertThatThrownBy(() -> wishService.remove(1L, 1L))
            .isInstanceOf(IllegalStateException.class);
    }
}
