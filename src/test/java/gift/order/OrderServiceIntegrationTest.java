package gift.order;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private KakaoMessageClient kakaoMessageClient;

    @Test
    @DisplayName("포인트 부족 시 재고가 롤백된다")
    void rollbackOnInsufficientPoints() {
        Category category = categoryRepository.save(new Category("롤백카테고리", "#000", "img.png", "설명"));
        Product product = productRepository.save(new Product("롤백상품", 1000, "img.png", category));
        Option option = optionRepository.save(new Option(product, "롤백옵션", 10));

        Member member = new Member("rollback@test.com", "pw");
        member.chargePoint(500);
        member = memberRepository.save(member);

        Long optionId = option.getId();
        Member finalMember = member;

        assertThatThrownBy(() -> orderService.createOrder(finalMember, new OrderRequest(optionId, 1, null)))
            .isInstanceOf(IllegalArgumentException.class);

        Option reloaded = optionRepository.findById(optionId).orElseThrow();
        assertThat(reloaded.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("정상 주문 시 재고와 포인트가 모두 차감된다")
    void successfulOrder() {
        Category category = categoryRepository.save(new Category("성공카테고리", "#000", "img.png", "설명"));
        Product product = productRepository.save(new Product("성공상품", 1000, "img.png", category));
        Option option = optionRepository.save(new Option(product, "성공옵션", 10));

        Member member = new Member("success@test.com", "pw");
        member.chargePoint(10_000);
        member = memberRepository.save(member);

        Long optionId = option.getId();

        Order order = orderService.createOrder(member, new OrderRequest(optionId, 3, "선물"));

        Option reloadedOption = optionRepository.findById(optionId).orElseThrow();
        Member reloadedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(reloadedOption.getQuantity()).isEqualTo(7);
        assertThat(reloadedMember.getPoint()).isEqualTo(7_000);
        assertThat(order.getQuantity()).isEqualTo(3);
    }
}
