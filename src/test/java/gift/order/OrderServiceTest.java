package gift.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("포인트 부족 시 재고 차감이 롤백된다")
    void createOrder_insufficientPoints_rollbacksStock() {
        // given: 포인트 1000원, 상품 가격 500원, 재고 10개
        Category category = categoryRepository.save(
            new Category("테스트", "#000000", "https://test.com/img.jpg", "설명"));
        Product product = productRepository.save(
            new Product("테스트상품", 500, "https://test.com/img.jpg", category));
        Option option = optionRepository.save(new Option(product, "기본옵션", 10));

        Member member = new Member("test@example.com", "password");
        member.chargePoint(1000);
        member = memberRepository.save(member);

        // when: 3개 주문 시도 (500 * 3 = 1500원 > 1000원 보유)
        Member finalMember = member;
        assertThatThrownBy(() ->
            orderService.createOrder(finalMember, option.getId(), 3, "선물")
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("포인트가 부족합니다.");

        // then: 재고가 원래대로 10개 유지되어야 한다 (롤백 증거)
        Option reloaded = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(reloaded.getQuantity()).isEqualTo(10);

        // then: 주문이 생성되지 않아야 한다
        assertThat(orderRepository.count()).isZero();

        // then: 포인트도 원래대로 1000원 유지되어야 한다
        Member reloadedMember = memberRepository.findById(finalMember.getId()).orElseThrow();
        assertThat(reloadedMember.getPoint()).isEqualTo(1000);
    }

    @Test
    @DisplayName("포인트와 재고가 충분하면 주문이 정상 생성된다")
    void createOrder_success() {
        // given
        Category category = categoryRepository.save(
            new Category("테스트", "#000000", "https://test.com/img.jpg", "설명"));
        Product product = productRepository.save(
            new Product("테스트상품", 500, "https://test.com/img.jpg", category));
        Option option = optionRepository.save(new Option(product, "기본옵션", 10));

        Member member = new Member("test@example.com", "password");
        member.chargePoint(5000);
        member = memberRepository.save(member);

        // when: 2개 주문 (500 * 2 = 1000원)
        Order order = orderService.createOrder(member, option.getId(), 2, "축하해!");

        // then: 주문 생성 확인
        assertThat(order.getId()).isNotNull();
        assertThat(order.getQuantity()).isEqualTo(2);

        // then: 재고 8개로 차감
        Option reloaded = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(reloaded.getQuantity()).isEqualTo(8);

        // then: 포인트 4000원으로 차감
        Member reloadedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(reloadedMember.getPoint()).isEqualTo(4000);
    }
}
