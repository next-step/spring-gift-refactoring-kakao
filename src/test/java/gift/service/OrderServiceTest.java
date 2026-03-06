package gift.service;

import gift.model.Category;
import gift.model.Member;
import gift.model.Option;
import gift.model.Order;
import gift.model.Product;
import gift.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OptionService optionService;
    @Mock
    private MemberService memberService;

    @InjectMocks
    private OrderService orderService;

    private Member member;
    private Option option;

    @BeforeEach
    void setUp() {
        member = new Member("test@example.com", "password");
        var category = new Category("카테고리", "#000000", "image.png", "설명");
        var product = new Product("상품", 1000, "image.png", category);
        option = new Option(product, "옵션A", 10);
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("성공 - 재고 차감, 포인트 차감, 주문 저장이 모두 수행된다")
        void success() {
            given(optionService.subtractQuantity(1L, 2)).willReturn(option);
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            Order result = orderService.createOrder(member, 1L, 2, "선물입니다");

            assertThat(result.getQuantity()).isEqualTo(2);
            assertThat(result.getMessage()).isEqualTo("선물입니다");
            then(optionService).should().subtractQuantity(1L, 2);
            then(memberService).should().deductPoint(member, 2000);
            then(orderRepository).should().save(any(Order.class));
        }

        @Test
        @DisplayName("포인트 부족 시 예외가 전파되고 주문이 저장되지 않는다")
        void failsWhenInsufficientPoints() {
            given(optionService.subtractQuantity(1L, 2)).willReturn(option);
            doThrow(new IllegalArgumentException("Insufficient points."))
                .when(memberService).deductPoint(member, 2000);

            assertThatThrownBy(() -> orderService.createOrder(member, 1L, 2, "선물입니다"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient points.");

            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("재고 부족 시 예외가 전파되고 포인트 차감 및 주문 저장이 수행되지 않는다")
        void failsWhenInsufficientStock() {
            given(optionService.subtractQuantity(1L, 100))
                .willThrow(new IllegalArgumentException("Subtract amount exceeds current stock."));

            assertThatThrownBy(() -> orderService.createOrder(member, 1L, 100, "선물입니다"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Subtract amount exceeds current stock.");

            then(memberService).should(never()).deductPoint(any(), anyInt());
            then(orderRepository).should(never()).save(any(Order.class));
        }
    }
}
