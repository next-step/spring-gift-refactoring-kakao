package gift.wish;

/**
 * 주문한 회원의 위시에서 주문한 상품을 뺀다(주문 흐름 6단계 "cleanup wish").
 */
public interface WishCleaner {
    void clean(Long memberId, Long productId);
}
