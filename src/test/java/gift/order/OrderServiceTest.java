package gift.order;

import gift.member.Member;
import gift.member.MemberService;
import gift.option.Option;
import gift.option.OptionService;
import gift.product.Product;
import gift.category.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OptionService optionService;

    @Mock
    private MemberService memberService;

    @Mock
    private KakaoMessageClient kakaoMessageClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_성공시_재고차감_포인트차감_주문저장이_모두_수행된다() {
        // given
        Category category = new Category("전자기기", "#000000", "url", "desc");
        Product product = new Product("상품", 1000, "img", category);
        Option option = new Option(product, "옵션A", 10);
        Member member = new Member("test@test.com", "password");

        given(optionService.subtractQuantity(1L, 3)).willReturn(option);
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        OrderRequest request = new OrderRequest(1L, 3, "선물입니다");

        // when
        Order result = orderService.createOrder(member, request);

        // then
        verify(optionService).subtractQuantity(1L, 3);
        verify(memberService).deductPoint(member.getId(), 3000);
        verify(orderRepository).save(any(Order.class));
        assertThat(result.getQuantity()).isEqualTo(3);
        assertThat(result.getMessage()).isEqualTo("선물입니다");
    }

    @Test
    void createOrder_포인트부족시_주문이_저장되지_않는다() {
        // given
        Category category = new Category("전자기기", "#000000", "url", "desc");
        Product product = new Product("상품", 1000, "img", category);
        Option option = new Option(product, "옵션A", 10);
        Member member = new Member("test@test.com", "password");

        given(optionService.subtractQuantity(1L, 3)).willReturn(option);
        willThrow(new IllegalArgumentException("포인트가 부족합니다."))
            .given(memberService).deductPoint(member.getId(), 3000);

        OrderRequest request = new OrderRequest(1L, 3, "선물입니다");

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(member, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("포인트가 부족합니다.");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_카카오메시지_실패해도_주문은_성공한다() {
        // given
        Category category = new Category("전자기기", "#000000", "url", "desc");
        Product product = new Product("상품", 1000, "img", category);
        Option option = new Option(product, "옵션A", 10);
        Member member = new Member("test@test.com", "password");
        member.updateKakaoAccessToken("kakao-token");

        given(optionService.subtractQuantity(1L, 1)).willReturn(option);
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
        willThrow(new RuntimeException("카카오 API 오류"))
            .given(kakaoMessageClient).sendToMe(any(), any());

        OrderRequest request = new OrderRequest(1L, 1, null);

        // when
        Order result = orderService.createOrder(member, request);

        // then
        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }
}