package gift.order;

import gift.category.Category;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private KakaoMessageClient kakaoMessageClient;

    @InjectMocks
    private OrderService orderService;

    private Member member;
    private Option option;
    private Product product;

    @BeforeEach
    void setUp() {
        Category category = new Category("카테고리", "#000", "img.png", "설명");
        product = new Product("상품", 1000, "img.png", category);
        option = new Option(product, "옵션A", 10);
        member = new Member("buyer@email.com", "pw");
        member.chargePoint(100_000);
    }

    @Test
    @DisplayName("정상 주문: 재고 차감 + 포인트 차감 + 주문 저장")
    void createOrderSuccess() {
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(member, new OrderRequest(1L, 3, "선물"));

        assertThat(option.getQuantity()).isEqualTo(7);
        assertThat(member.getPoint()).isEqualTo(100_000 - 1000 * 3);
        assertThat(order.getQuantity()).isEqualTo(3);
        assertThat(order.getMessage()).isEqualTo("선물");
        verify(optionRepository).save(option);
        verify(memberRepository).save(member);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("존재하지 않는 옵션이면 예외가 발생한다")
    void optionNotFound() {
        given(optionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(member, new OrderRequest(999L, 1, null)))
            .isInstanceOf(NoSuchElementException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("재고 부족이면 예외가 발생하고 주문이 저장되지 않는다")
    void insufficientStock() {
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));

        assertThatThrownBy(() -> orderService.createOrder(member, new OrderRequest(1L, 11, null)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("포인트 부족이면 예외가 발생하고 주문이 저장되지 않는다")
    void insufficientPoints() {
        Member poorMember = new Member("poor@email.com", "pw");
        poorMember.chargePoint(500);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));

        assertThatThrownBy(() -> orderService.createOrder(poorMember, new OrderRequest(1L, 1, null)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("카카오 알림이 실패해도 주문은 성공한다")
    void kakaoFailureDoesNotBreakOrder() {
        member.updateKakaoAccessToken("kakao-token");
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("카카오 API 오류"))
            .when(kakaoMessageClient).sendToMe(any(), any(), any());

        Order order = orderService.createOrder(member, new OrderRequest(1L, 1, "메시지"));

        assertThat(order).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }
}
