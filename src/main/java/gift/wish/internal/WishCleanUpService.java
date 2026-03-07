package gift.wish.internal;

import gift.global.NotFoundException;
import gift.wish.Wish;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishCleanUpService {

    private final WishRepository wishRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanWishByMemberAndProductId(Long memberId, Long productId) {
        Wish find = wishRepo.findByMemberIdAndProductId(memberId, productId)
                .orElseThrow(NotFoundException::wishNotFound);

        wishRepo.delete(find);
    }
}
