package gift.wish;

import gift.auth.exception.AuthenticationException;
import gift.auth.jwt.AuthenticationResolver;
import gift.auth.exception.ForbiddenException;
import gift.category.entity.Category;
import gift.member.entity.Member;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.wish.dto.AddWishResult;
import gift.wish.dto.WishRequest;
import gift.wish.entity.Wish;
import gift.wish.exception.WishErrorCode;
import gift.wish.exception.WishException;
import gift.wish.repository.WishRepository;
import gift.wish.service.WishService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishServiceTest {
    @Mock
    private WishRepository wishRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuthenticationResolver authenticationResolver;

    @InjectMocks
    private WishService wishService;

    @Test
    @DisplayName("인증 정보가 없으면 AuthenticationException 예외를 던진다")
    void addWish_authFailed_throwsException() {
        when(authenticationResolver.extractMember("Bearer token")).thenReturn(null);

        assertThrows(AuthenticationException.class, () -> wishService.addWish("Bearer token", new WishRequest(1L)));
    }

    @Test
    @DisplayName("상품이 없으면 PRODUCT_NOT_FOUND 예외를 던진다")
    void addWish_productNotFound_throwsException() {
        Member member = new Member("test@example.com", "password");
        when(authenticationResolver.extractMember("Bearer token")).thenReturn(member);
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        WishException exception = assertThrows(
            WishException.class,
            () -> wishService.addWish("Bearer token", new WishRequest(1L))
        );

        assertEquals(WishErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미 위시가 존재하면 created=false를 반환한다")
    void addWish_alreadyExists_returnsCreatedFalse() {
        Member member = new Member("test@example.com", "password");
        ReflectionTestUtils.setField(member, "id", 1L);
        Product product = new Product("아메리카노", 3000, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        ReflectionTestUtils.setField(product, "id", 10L);
        Wish existing = new Wish(1L, product);

        when(authenticationResolver.extractMember("Bearer token")).thenReturn(member);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(wishRepository.findByMemberIdAndProductId(1L, 10L)).thenReturn(Optional.of(existing));

        AddWishResult result = wishService.addWish("Bearer token", new WishRequest(1L));

        assertFalse(result.created());
    }

    @Test
    @DisplayName("새 위시를 추가하면 created=true를 반환한다")
    void addWish_newWish_returnsCreatedTrue() {
        Member member = new Member("test@example.com", "password");
        ReflectionTestUtils.setField(member, "id", 1L);
        Product product = new Product("아메리카노", 3000, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        ReflectionTestUtils.setField(product, "id", 10L);
        Wish savedWish = new Wish(1L, product);

        when(authenticationResolver.extractMember("Bearer token")).thenReturn(member);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(wishRepository.findByMemberIdAndProductId(any(), any())).thenReturn(Optional.empty());
        when(wishRepository.save(any(Wish.class))).thenReturn(savedWish);

        AddWishResult result = wishService.addWish("Bearer token", new WishRequest(1L));

        assertTrue(result.created());
    }

    @Test
    @DisplayName("위시 삭제 시 위시가 없으면 WISH_NOT_FOUND 예외를 던진다")
    void removeWish_notFound_throwsException() {
        Member member = new Member("test@example.com", "password");
        ReflectionTestUtils.setField(member, "id", 1L);
        when(authenticationResolver.extractMember("Bearer token")).thenReturn(member);
        when(wishRepository.findById(1L)).thenReturn(Optional.empty());

        WishException exception = assertThrows(
            WishException.class,
            () -> wishService.removeWish("Bearer token", 1L)
        );

        assertEquals(WishErrorCode.WISH_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("위시 소유자가 아니면 ForbiddenException 예외를 던진다")
    void removeWish_forbidden_throwsException() {
        Member member = new Member("test@example.com", "password");
        ReflectionTestUtils.setField(member, "id", 1L);
        Product product = new Product("아메리카노", 3000, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        Wish wish = new Wish(999L, product);

        when(authenticationResolver.extractMember("Bearer token")).thenReturn(member);
        when(wishRepository.findById(1L)).thenReturn(Optional.of(wish));

        assertThrows(ForbiddenException.class, () -> wishService.removeWish("Bearer token", 1L));
    }

    @Test
    @DisplayName("위시 삭제 성공 시 delete를 호출한다")
    void removeWish_success_deletesWish() {
        Member member = new Member("test@example.com", "password");
        ReflectionTestUtils.setField(member, "id", 1L);
        Product product = new Product("아메리카노", 3000, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        Wish wish = new Wish(1L, product);

        when(authenticationResolver.extractMember("Bearer token")).thenReturn(member);
        when(wishRepository.findById(1L)).thenReturn(Optional.of(wish));

        wishService.removeWish("Bearer token", 1L);

        verify(wishRepository).delete(wish);
    }
}
