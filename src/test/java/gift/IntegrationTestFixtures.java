package gift;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import gift.wish.Wish;
import gift.wish.WishRepository;

public class IntegrationTestFixtures {

    public static Category savedCategory(CategoryRepository repo) {
        return repo.save(new Category("교환권", "#ffffff", "http://img.test/category.png", "테스트 카테고리"));
    }

    public static Product savedProduct(ProductRepository repo, Category category) {
        return repo.save(new Product("테스트 상품", 10000, "http://img.test/product.png", category));
    }

    public static Option savedOption(OptionRepository repo, Product product) {
        return repo.save(new Option(product, "기본 옵션", 100));
    }

    public static Member savedMemberWithPoints(MemberRepository repo, String email, int points) {
        Member member = repo.save(new Member(email, "password"));
        member.chargePoint(points);
        return repo.save(member);
    }

    public static Wish savedWish(WishRepository repo, Long memberId, Product product) {
        return repo.save(new Wish(memberId, product));
    }
}
