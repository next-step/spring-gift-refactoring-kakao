package gift.order;

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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class OrderServiceTransactionTest {

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

    @MockitoSpyBean
    private OrderRepository orderRepository;

    private Option option;
    private Member member;

    @BeforeEach
    void setUp() {
        var category = categoryRepository.save(new Category("트랜잭션테스트", "#fff", "img.png", ""));
        var product = productRepository.save(new Product("테스트상품", 5000, "img.png", category));
        option = optionRepository.save(new Option(product, "기본옵션", 100));

        member = new Member("txtest@test.com", "password");
        member.chargePoint(100000);
        member = memberRepository.save(member);
    }

    @Test
    @DisplayName("주문 저장 실패 시 재고와 포인트가 롤백된다")
    void rollsBackOnOrderSaveFailure() {
        doThrow(new RuntimeException("DB 저장 실패"))
            .when(orderRepository).save(any(Order.class));

        var request = new OrderRequest(option.getId(), 2, "선물");

        assertThatThrownBy(() -> orderService.createOrder(member, request))
            .isInstanceOf(RuntimeException.class);

        // 재조회하여 롤백 검증 — 상태가 변하지 않았어야 한다
        Option freshOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(freshOption.getQuantity()).isEqualTo(100);

        Member freshMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(freshMember.getPoint()).isEqualTo(100000);
    }
}
