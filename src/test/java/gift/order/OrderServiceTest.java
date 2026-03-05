package gift.order;

import gift.auth.AuthenticationException;
import gift.auth.AuthenticationResolver;
import gift.category.Category;
import gift.kakao.KakaoMessageClient;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AuthenticationResolver authenticationResolver;

    @Mock
    private KakaoMessageClient kakaoMessageClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("인증 정보가 없으면 AuthenticationException 예외를 던진다")
    void createOrder_authFailed_throwsException() {
        when(authenticationResolver.extractMember("Bearer token")).thenReturn(null);

        assertThrows(AuthenticationException.class, () -> orderService.createOrder("Bearer token", new OrderRequest(1L, 1, "msg")));
    }

    @Test
    @DisplayName("옵션이 없으면 OPTION_NOT_FOUND 예외를 던진다")
    void createOrder_optionNotFound_throwsException() {
        Member member = new Member("test@example.com", "password");
        when(authenticationResolver.extractMember("Bearer token")).thenReturn(member);
        when(optionRepository.findById(1L)).thenReturn(Optional.empty());

        OrderException exception = assertThrows(
            OrderException.class,
            () -> orderService.createOrder("Bearer token", new OrderRequest(1L, 1, "msg"))
        );

        assertEquals(OrderErrorCode.OPTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("주문 성공 시 주문 응답을 반환한다")
    void createOrder_success_returnsResponse() {
        Member member = new Member("test@example.com", "password");
        member.chargePoint(10000);
        Product product = new Product("아메리카노", 3000, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        Option option = new Option(product, "기본 옵션", 10);
        OrderRequest request = new OrderRequest(1L, 2, "메시지");
        Order savedOrder = new Order(option, 1L, request.quantity(), request.message());

        when(authenticationResolver.extractMember("Bearer token")).thenReturn(member);
        when(optionRepository.findById(request.optionId())).thenReturn(Optional.of(option));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder("Bearer token", request);

        assertEquals(request.quantity(), response.quantity());
        assertEquals(request.message(), response.message());
        verify(optionRepository).save(option);
        verify(memberRepository).save(member);
        verify(orderRepository).save(any(Order.class));
    }
}
