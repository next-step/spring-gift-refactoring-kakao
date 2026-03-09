package gift;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.order.OrderRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import gift.wish.WishRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ServiceTestFixture {

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected OptionRepository optionRepository;

    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected WishRepository wishRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @BeforeEach
    protected void setUp() {
        orderRepository.deleteAll();
        wishRepository.deleteAll();
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    protected Option createOption(int productPrice, int stockQuantity) {
        var category = categoryRepository.save(new Category("테스트", "#000", "http://img.test/c.png", null));
        var product = productRepository.save(new Product("테스트상품", productPrice, "http://img.test/p.png", category));
        return optionRepository.save(new Option(product, "기본옵션", stockQuantity));
    }

    protected Member createMember(String email, int point) {
        var member = new Member(email, "password");
        if (point > 0) {
            member.chargePoint(point);
        }
        return memberRepository.save(member);
    }

    protected Member createKakaoMember(String email, int point) {
        var member = createMember(email, point);
        member.updateKakaoAccessToken("kakao-token");
        return memberRepository.save(member);
    }
}
