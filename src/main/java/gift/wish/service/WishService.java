package gift.wish.service;

import gift.auth.exception.AuthenticationException;
import gift.auth.jwt.AuthenticationResolver;
import gift.member.entity.Member;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
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
    private final ProductRepository productRepository;
    private final AuthenticationResolver authenticationResolver;

    public Page<WishResponse> getWishes(String authorization, Pageable pageable) {
        Member member = extractMember(authorization);
        return wishRepository.findByMemberId(member.getId(), pageable).map(WishResponse::from);
    }

    @Transactional
    public AddWishResult addWish(String authorization, WishRequest request) {
        Member member = extractMember(authorization);
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new WishException(WishErrorCode.PRODUCT_NOT_FOUND));
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

    private Member extractMember(String authorization) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            throw new AuthenticationException();
        }
        return member;
    }
}
