package gift.support;

import gift.category.Category;
import gift.member.Member;
import gift.option.Option;
import gift.order.Order;
import gift.product.Product;
import gift.wish.Wish;

/**
 * 데이터를 추가, 삭제할 수 있는 계약
 * <p>
 * 제공되는 entity 는 모두 영속 context 에서 분리된 객체
 */
public interface DataManipulator {

    Category addCategory(String name, String color, String imageUrl, String description);

    Member addMember(String email, String password, String kakaoAccessToken, int point);

    Option addOption(Long productId, String name, int quantity);

    Order addOrder(Long optionId, Long memberId, int quantity, String message);

    Product addProduct(String name, int price, String imageUrl, Long categoryId);

    Wish addWish(Long memberId, Long productId);

    void initAll();
}
