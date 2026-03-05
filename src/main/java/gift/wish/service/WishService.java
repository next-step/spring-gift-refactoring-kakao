package gift.wish.service;

import gift.auth.exception.AuthenticationException;
import gift.auth.jwt.AuthenticationResolver;
import gift.member.entity.Member;
import gift.product.entity.Product;
import gift.product.exception.ProductErrorCode;
import gift.product.exception.ProductException;
import gift.product.service.ProductService;
import gift.wish.dto.AddWishResult;
import gift.wish.dto.WishRequest;
import gift.wish.dto.WishResponse;
import gift.wish.entity.Wish;
import gift.wish.exception.WishErrorCode;
import gift.wish.exception.WishException;
import gift.wish.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishService {
    private final WishRepository wishRepository;
    private final ProductService productService;
    private final AuthenticationResolver authenticationResolver;

    public Page<WishResponse> getWishes(String authorization, Pageable pageable) {
        Member member = extractMember(authorization);
        return wishRepository.findByMemberId(member.getId(), pageable).map(WishResponse::from);
    }

    @Transactional
    public AddWishResult addWish(String authorization, WishRequest request) {
        Member member = extractMember(authorization);
        Product product = findProductOrThrow(request.productId());
        return wishRepository.findByMemberIdAndProductId(member.getId(), product.getId())
            .map(existing -> new AddWishResult(WishResponse.from(existing), false))
            .orElseGet(() -> new AddWishResult(
                WishResponse.from(wishRepository.save(request.toEntity(member.getId(), product))), true));
    }

    @Transactional
    public void removeWish(String authorization, Long id) {
        Member member = extractMember(authorization);
        Wish wish = wishRepository.findById(id)
            .orElseThrow(() -> new WishException(WishErrorCode.WISH_NOT_FOUND));
        wish.assertOwner(member.getId());
        wishRepository.delete(wish);
    }

    @Transactional
    public void removeWishByMemberAndProduct(Long memberId, Long productId) {
        wishRepository.deleteByMemberIdAndProductId(memberId, productId);
    }

    private Member extractMember(String authorization) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            throw new AuthenticationException();
        }
        return member;
    }

    private Product findProductOrThrow(Long productId) {
        try {
            return productService.findByIdOrThrow(productId);
        } catch (ProductException exception) {
            if (exception.getErrorCode() == ProductErrorCode.PRODUCT_NOT_FOUND) {
                throw new WishException(WishErrorCode.PRODUCT_NOT_FOUND);
            }
            throw exception;
        }
    }
}
