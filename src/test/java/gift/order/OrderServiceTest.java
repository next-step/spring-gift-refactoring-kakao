package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.wish.WishRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private WishRepository wishRepository;

    @Mock
    private OrderNotificationSender notificationSender;

    @Mock
    private TransactionTemplate transactionTemplate;

    private OrderService orderService;

    private Product product;
    private Option option;
    private Member member;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        given(transactionTemplate.execute(any(TransactionCallback.class)))
            .willAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });

        orderService = new OrderService(
            orderRepository, optionRepository, memberRepository, wishRepository, notificationSender, transactionTemplate
        );

        product = new Product("아메리카노", 4500, "http://img.url", null);
        option = new Option(product, "ICE", 10);
        member = new Member("test@test.com", "password");
        member.chargePoint(100000);
    }

    @Test
    @DisplayName("정상 주문 시 재고 차감, 포인트 차감, 주문 저장이 수행된다")
    void createOrder() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(optionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.createOrder(1L, 1L, 3, "선물입니다");

        assertThat(option.getQuantity()).isEqualTo(7);
        assertThat(member.getPoint()).isEqualTo(100000 - 4500 * 3);
        assertThat(order.getQuantity()).isEqualTo(3);
        then(orderRepository).should().save(any(Order.class));
    }

    @Test
    @DisplayName("존재하지 않는 옵션으로 주문하면 NoSuchElementException이 발생한다")
    void createOrderWithNonExistentOption() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(optionRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(1L, 999L, 1, ""))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("재고보다 많은 수량을 주문하면 IllegalArgumentException이 발생한다")
    void createOrderExceedingStock() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(optionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(option));

        assertThatThrownBy(() -> orderService.createOrder(1L, 1L, 11, ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("포인트가 부족하면 IllegalArgumentException이 발생한다")
    void createOrderInsufficientPoint() {
        Member poorMember = new Member("poor@test.com", "password");
        poorMember.chargePoint(100);
        given(memberRepository.findById(2L)).willReturn(Optional.of(poorMember));
        given(optionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(option));

        assertThatThrownBy(() -> orderService.createOrder(2L, 1L, 1, ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("주문 완료 후 알림 전송이 호출된다")
    void createOrderSendsNotification() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(optionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrder(1L, 1L, 1, "");

        then(notificationSender).should().send(any(Member.class), any(Order.class), any(Option.class));
    }

    @Test
    @DisplayName("주문 완료 후 해당 상품의 위시가 삭제된다")
    void createOrderDeletesWish() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(optionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrder(1L, 1L, 1, "");

        then(wishRepository).should().deleteByMemberIdAndProductId(1L, product.getId());
    }
}
