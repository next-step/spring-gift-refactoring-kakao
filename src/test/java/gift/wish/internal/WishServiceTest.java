package gift.wish.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import gift.global.ForbiddenException;
import gift.global.NotFoundException;
import gift.product.Product;
import gift.product.ProductQueryPort;
import gift.wish.Wish;
import java.lang.reflect.Field;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PagedModel.PageMetadata;

@ExtendWith(MockitoExtension.class)
class WishServiceTest {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    @InjectMocks
    WishService wishService;

    @Mock
    WishRepository wishRepo;

    @Mock
    ProductQueryPort productQueryPort;

    @Test
    @DisplayName("위시 목록을 조회한다 — wishRepo.findByMemberId 호출 + 응답 매핑")
    void testGetWishes() {
        // given
        Long memberId = 1L;
        int pageNumber = 0;
        Pageable pageable = PageRequest.of(pageNumber, 10);

        Product product = createProduct(10L, "상품", 1000, "https://img.png");
        Wish wish = createWish(100L, memberId, product);

        Page<Wish> page = new PageImpl<>(
                List.of(wish), pageable, 1
        );

        given(wishRepo.findByMemberId(memberId, pageable))
                .willReturn(page);

        // when
        PagedModel<WishResponse> response = wishService.getWishes(memberId, pageable);

        // then
        then(wishRepo).should()
                .findByMemberId(memberId, pageable);

        assertThat(response.getContent()).hasSize(1);

        PageMetadata metadata = response.getMetadata();
        assertThat(metadata.size()).isEqualTo(10);
        assertThat(metadata.number()).isEqualTo(pageNumber);
        assertThat(metadata.totalElements()).isOne();
        assertThat(metadata.totalPages()).isOne();

        WishResponse content = response.getContent().getFirst();
        assertThat(content.id()).isEqualTo(wish.getId());
        assertThat(content.productId()).isEqualTo(product.getId());
        assertThat(content.name()).isEqualTo(product.getName());
        assertThat(content.price()).isEqualTo(product.getPrice());
        assertThat(content.imageUrl()).isEqualTo(product.getImageUrl());
    }

    @SuppressWarnings("SameParameterValue")
    private static Product createProduct(Long id, String name, int price, String imageUrl) {
        Product product = Product.builder()
                .name(name)
                .price(price)
                .imageUrl(imageUrl)
                .build();

        setId(product, Product.class, id);
        return product;
    }

    private static Wish createWish(Long id, Long memberId, Product product) {
        Wish wish = Wish.builder()
                .memberId(memberId)
                .product(product)
                .build();

        setId(wish, Wish.class, id);
        return wish;
    }

    private static <T> void setId(T entity, Class<T> clazz, Long id) {
        try {
            Field idField = clazz.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    @DisplayName("위시를 추가한다 — productQueryPort.getReference 호출 + 중복 없음 → save + isCreated=true")
    void testAddWishNew() {
        // given
        Long memberId = 1L;
        Long productId = 10L;
        Product product = createProduct(productId, "상품", 1000, "https://img.png");
        Wish savedWish = createWish(100L, memberId, product);

        given(productQueryPort.getReference(productId))
                .willReturn(product);
        given(wishRepo.findByMemberIdAndProductId(memberId, productId))
                .willReturn(Optional.empty());
        given(wishRepo.save(any(Wish.class)))
                .willReturn(savedWish);

        // when
        AddWishResponseDto response = wishService.addWish(memberId, new WishRequest(productId));

        // then
        then(productQueryPort).should()
                .getReference(productId);
        then(wishRepo).should()
                .save(any(Wish.class));

        assertThat(response.created()).isTrue();

        WishResponse wishResponse = response.response();

        assertThat(wishResponse.id()).isEqualTo(savedWish.getId());

        assertThat(wishResponse.productId()).isEqualTo(productId);
        assertThat(wishResponse.name()).isEqualTo(product.getName());
        assertThat(wishResponse.price()).isEqualTo(product.getPrice());
        assertThat(wishResponse.imageUrl()).isEqualTo(product.getImageUrl());
    }

    @Test
    @DisplayName("이미 존재하는 위시를 추가한다 — 중복 있음 → save 안 함 + isCreated=false")
    void testAddWishDuplicate() {
        // given
        Long memberId = 1L;
        Long productId = 10L;
        Product product = createProduct(productId, "상품", 1000, "https://img.png");
        Wish existingWish = createWish(100L, memberId, product);

        given(productQueryPort.getReference(productId))
                .willReturn(product);
        given(wishRepo.findByMemberIdAndProductId(memberId, productId))
                .willReturn(Optional.of(existingWish));

        // when
        AddWishResponseDto response = wishService.addWish(memberId, new WishRequest(productId));

        // then
        then(wishRepo).shouldHaveNoMoreInteractions();

        assertThat(response.created()).isFalse();
        assertThat(response.response().id()).isEqualTo(existingWish.getId());
    }

    @Test
    @DisplayName("위시 추가 시 상품이 없으면 NotFoundException 이 전파된다")
    void testAddWishProductNotFound() {
        // given
        Long memberId = 1L;
        given(productQueryPort.getReference(NOT_EXISTING_ID))
                .willThrow(NotFoundException.productNotFound());

        // when + then
        assertThatThrownBy(() -> wishService.addWish(memberId, new WishRequest(NOT_EXISTING_ID)))
                .isInstanceOf(NotFoundException.class);
    }

    // -- fixtures --

    @Test
    @DisplayName("위시를 삭제한다 — 소유자 일치 → wishRepo.delete 호출")
    void testRemoveWish() {
        // given
        Long memberId = 1L;
        Long wishId = 100L;
        Product product = createProduct(10L, "상품", 1000, "https://img.png");
        Wish wish = createWish(wishId, memberId, product);

        given(wishRepo.findById(wishId))
                .willReturn(Optional.of(wish));

        // when
        wishService.removeWish(memberId, wishId);

        // then
        then(wishRepo).should()
                .delete(wish);
    }

    @Test
    @DisplayName("존재하지 않는 위시 삭제 시 NotFoundException 이 발생한다")
    void testRemoveWishNotFound() {
        // given
        Long memberId = 1L;
        given(wishRepo.findById(NOT_EXISTING_ID))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> wishService.removeWish(memberId, NOT_EXISTING_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("다른 사용자의 위시 삭제 시 ForbiddenException 이 발생한다")
    void testRemoveWishForbidden() {
        // given
        Long ownerMemberId = 1L;
        Long otherMemberId = 2L;
        Long wishId = 100L;
        Product product = createProduct(10L, "상품", 1000, "https://img.png");
        Wish wish = createWish(wishId, ownerMemberId, product);

        given(wishRepo.findById(wishId))
                .willReturn(Optional.of(wish));

        // when + then
        assertThatThrownBy(() -> wishService.removeWish(otherMemberId, wishId))
                .isInstanceOf(ForbiddenException.class);
    }
}
