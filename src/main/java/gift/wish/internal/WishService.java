package gift.wish.internal;

import gift.global.ForbiddenException;
import gift.global.NotFoundException;
import gift.product.Product;
import gift.product.ProductQueryPort;
import gift.wish.Wish;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishService {

    private final WishRepository wishRepo;
    private final ProductQueryPort productQueryPort;

    public PagedModel<WishResponse> getWishes(Long memberId, Pageable pageable) {
        Page<WishResponse> pageResponse = wishRepo.findByMemberIdInnerJoinFetchProduct(
                        memberId, pageable
                )
                .map(WishResponse::from);

        return new PagedModel<>(pageResponse);
    }


    @Transactional
    public AddWishResponseDto addWish(Long memberId, WishRequest request) {
        Long productId = request.productId();

        Product product = productQueryPort.getReference(productId);

        Optional<Wish> opt = wishRepo.findByMemberIdAndProductIdInnerJoinFetchProduct(
                memberId, productId
        );

        if (opt.isPresent()) {
            Wish entity = opt.get();
            WishResponse wishResponse = WishResponse.from(entity);
            return new AddWishResponseDto(wishResponse, false);
        }

        Wish build = Wish.builder()
                .product(product)
                .memberId(memberId)
                .build();

        Wish newEntity = wishRepo.save(build);
        WishResponse wishResponse = WishResponse.from(newEntity);

        return new AddWishResponseDto(wishResponse, true);
    }

    @Transactional
    public void removeWish(Long memberId, Long wishId) {
        Wish find = wishRepo.findById(wishId)
                .orElseThrow(NotFoundException::wishNotFound);

        if (!find.getMemberId().equals(memberId)) {
            throw new ForbiddenException();
        }

        wishRepo.delete(find);
    }
}
