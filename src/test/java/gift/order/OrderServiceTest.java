package gift.order;

import gift.member.Member;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.wish.WishRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private WishRepository wishRepository;

    @Mock
    private OrderMessageClient messageClient;

    @InjectMocks
    private OrderService orderService;

    private Product product;
    private Option option;
    private Member member;

    @BeforeEach
    void setUp() {
        product = new Product("아메리카노", 4500, "http://img.url", null);
        option = new Option(product, "ICE", 10);
        member = new Member("test@test.com", "password");
        member.chargePoint(100000);
    }

    @Test
    @DisplayName("정상 주문 시 재고 차감, 포인트 차감, 주문 저장이 수행된다")
    void createOrder() {
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.createOrder(member, 1L, 3, "선물입니다");

        assertThat(option.getQuantity()).isEqualTo(7);
        assertThat(member.getPoint()).isEqualTo(100000 - 4500 * 3);
        assertThat(order.getQuantity()).isEqualTo(3);
        then(orderRepository).should().save(any(Order.class));
    }

    @Test
    @DisplayName("존재하지 않는 옵션으로 주문하면 NoSuchElementException이 발생한다")
    void createOrderWithNonExistentOption() {
        given(optionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(member, 999L, 1, ""))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("재고보다 많은 수량을 주문하면 IllegalArgumentException이 발생한다")
    void createOrderExceedingStock() {
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));

        assertThatThrownBy(() -> orderService.createOrder(member, 1L, 11, ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("포인트가 부족하면 IllegalArgumentException이 발생한다")
    void createOrderInsufficientPoint() {
        Member poorMember = new Member("poor@test.com", "password");
        poorMember.chargePoint(100);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));

        assertThatThrownBy(() -> orderService.createOrder(poorMember, 1L, 1, ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("카카오 토큰이 없는 회원은 카카오 메시지를 전송하지 않는다")
    void createOrderWithoutKakaoToken() {
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrder(member, 1L, 1, "");

        then(messageClient).should(never()).sendToMe(anyString(), any(Order.class), any(Product.class));
    }

    @Test
    @DisplayName("카카오 메시지 전송에 실패해도 주문은 정상 저장된다")
    void createOrderKakaoMessageFails() {
        member.updateOAuthAccessToken("kakao-token");
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("카카오 API 오류"))
            .when(messageClient).sendToMe(eq("kakao-token"), any(Order.class), any(Product.class));

        Order order = orderService.createOrder(member, 1L, 2, "선물");

        assertThat(order.getQuantity()).isEqualTo(2);
        then(orderRepository).should().save(any(Order.class));
    }
}
