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
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrderTotalPriceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Option option;
    private Member member;

    @BeforeEach
    void setUp() {
        var category = categoryRepository.save(new Category("가격테스트", "#000", "img.png", ""));
        var product = productRepository.save(new Product("테스트상품", 5000, "img.png", category));
        option = optionRepository.save(new Option(product, "기본옵션", 100));

        member = new Member("price-test@test.com", "password");
        member.chargePoint(100000);
        member = memberRepository.save(member);
    }

    @Test
    @DisplayName("주문 생성 시 totalPrice가 price × quantity로 저장된다")
    void savesTotalPriceOnOrderCreation() {
        var request = new OrderRequest(option.getId(), 2, "선물");

        Order order = orderService.createOrder(member, request);

        // 1차 캐시를 비우고 DB에서 재조회
        entityManager.flush();
        entityManager.clear();

        Order freshOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(freshOrder.getTotalPrice()).isEqualTo(10000); // 5000 * 2
    }

    @Test
    @DisplayName("수량이 1일 때 totalPrice는 단가와 같다")
    void totalPriceEqualsUnitPriceForQuantityOne() {
        var request = new OrderRequest(option.getId(), 1, "");

        Order order = orderService.createOrder(member, request);

        entityManager.flush();
        entityManager.clear();

        Order freshOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(freshOrder.getTotalPrice()).isEqualTo(5000); // 5000 * 1
    }
}
