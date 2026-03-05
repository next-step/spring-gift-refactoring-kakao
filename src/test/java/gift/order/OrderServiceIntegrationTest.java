package gift.order;

import gift.IntegrationTestWithoutTx;
import gift.IntegrationTestFixtures;
import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import gift.wish.WishRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;

@IntegrationTestWithoutTx
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private WishRepository wishRepository;

    @MockitoBean
    private KakaoMessageClient kakaoMessageClient;

    private Member member;
    private Option option;
    private Product product;
    private Category category;

    @BeforeEach
    void setUp() {
        category = IntegrationTestFixtures.savedCategory(categoryRepository);
        product = IntegrationTestFixtures.savedProduct(productRepository, category);
        option = IntegrationTestFixtures.savedOption(optionRepository, product);
        member = IntegrationTestFixtures.savedMemberWithPoints(memberRepository, "order@test.com", 1_000_000);
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAllInBatch();
        wishRepository.deleteAllInBatch();
        optionRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    void placeOrder_happyPath_persistsOrderAndDeductsStock() {
        var request = new OrderRequest(option.getId(), 5, "테스트 주문");

        Order order = orderService.placeOrder(member, request);

        assertThat(order.getId()).isNotNull();
        assertThat(order.getQuantity()).isEqualTo(5);

        Option refreshedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(refreshedOption.getQuantity()).isEqualTo(95);

        Member refreshedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(refreshedMember.getPoint()).isEqualTo(1_000_000 - product.getPrice() * 5);
    }

    @Test
    void placeOrder_insufficientStock_rollsBackCompletely() {
        var request = new OrderRequest(option.getId(), 999, "재고 초과");

        assertThatThrownBy(() -> orderService.placeOrder(member, request))
            .isInstanceOf(IllegalArgumentException.class);

        Option refreshedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(refreshedOption.getQuantity()).isEqualTo(100);

        Member refreshedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(refreshedMember.getPoint()).isEqualTo(1_000_000);
    }

    @Test
    void placeOrder_insufficientPoints_rollsBackCompletely() {
        Member poorMember = IntegrationTestFixtures.savedMemberWithPoints(memberRepository, "poor@test.com", 1);
        var request = new OrderRequest(option.getId(), 1, "포인트 부족");

        assertThatThrownBy(() -> orderService.placeOrder(poorMember, request))
            .isInstanceOf(IllegalArgumentException.class);

        Option refreshedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(refreshedOption.getQuantity()).isEqualTo(100);

        Member refreshedMember = memberRepository.findById(poorMember.getId()).orElseThrow();
        assertThat(refreshedMember.getPoint()).isEqualTo(1);
    }

    @Test
    void placeOrder_withKakaoToken_sendsMessage() {
        Member kakaoMember = memberRepository.save(new Member("kakao@test.com"));
        kakaoMember.updateKakaoAccessToken("test-kakao-token");
        kakaoMember.chargePoint(1_000_000);
        kakaoMember = memberRepository.save(kakaoMember);

        var request = new OrderRequest(option.getId(), 1, "카카오 주문");

        Order order = orderService.placeOrder(kakaoMember, request);

        assertThat(order.getId()).isNotNull();
        assertThat(orderRepository.findById(order.getId())).isPresent();

        Option refreshedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(refreshedOption.getQuantity()).isEqualTo(99);

        then(kakaoMessageClient).should().sendToMe(anyString(), any(Order.class), any(Product.class));
    }
}
