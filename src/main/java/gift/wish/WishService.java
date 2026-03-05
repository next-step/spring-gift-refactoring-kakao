package gift.wish;

import gift.common.exception.ApplicationException;
import gift.member.Member;
import gift.member.MemberService;
import gift.product.Product;
import gift.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class WishService {
    private final WishRepository wishRepository;
    private final ProductService productService;
    private final MemberService memberService;

    public Page<Wish> findByMemberId(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable);
    }

    public Optional<Wish> findByMemberIdAndProductId(Long memberId, Long productId) {
        return wishRepository.findByMemberIdAndProductId(memberId, productId);
    }

    @Transactional
    public Wish create(Long memberId, Long productId) {
        Member member = memberService.findById(memberId);
        Product product = productService.findById(productId);
        return wishRepository.save(new Wish(member, product));
    }

    @Transactional
    public void removeWish(Long id, Long memberId) {
        Wish wish = wishRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(WishErrorCode.NOT_FOUND));

        wish.validateOwner(memberId);

        wishRepository.delete(wish);
    }
}
