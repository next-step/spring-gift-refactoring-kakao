package gift.order;

import gift.category.Category;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.wish.WishRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OrderCommandServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private WishRepository wishRepository;

    @Mock
    private KakaoMessageClient kakaoMessageClient;

    @InjectMocks
    private OrderCommandService orderCommandService;

    private Member createMemberWithIdAndPoint(Long id, int point) {
        var member = new Member("test@example.com", "password");
        member.chargePoint(point);
        try {
            Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return member;
    }

    private Option createOptionWithProductId(Long productId, int price, int quantity) {
        var category = new Category("전자기기", "#000", "https://example.com/img.jpg", "설명");
        var product = new Product("테스트상품", price, "https://example.com/img.jpg", category);
        try {
            Field idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(product, productId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new Option(product, "옵션A", quantity);
    }

    @Test
    @DisplayName("주문을 생성한다")
    void createOrder() {
        var member = createMemberWithIdAndPoint(1L, 100000);
        var option = createOptionWithProductId(1L, 10000, 10);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
        given(wishRepository.findByMemberIdAndProductId(1L, 1L)).willReturn(Optional.empty());

        Order result = orderCommandService.createOrder(member, 1L, 2, "선물");

        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(option.getQuantity()).isEqualTo(8);
        assertThat(member.getPoint()).isEqualTo(80000);
    }

    @Test
    @DisplayName("존재하지 않는 옵션으로 주문하면 예외가 발생한다")
    void createOrder_OptionNotFound() {
        var member = createMemberWithIdAndPoint(1L, 100000);
        given(optionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderCommandService.createOrder(member, 999L, 1, "메시지"))
            .isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("재고 부족 시 예외가 발생한다")
    void createOrder_InsufficientStock() {
        var member = createMemberWithIdAndPoint(1L, 1000000);
        var option = createOptionWithProductId(1L, 1000, 5);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));

        assertThatThrownBy(() -> orderCommandService.createOrder(member, 1L, 10, "메시지"))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("포인트 부족 시 예외가 발생한다")
    void createOrder_InsufficientPoint() {
        var member = createMemberWithIdAndPoint(1L, 1000);
        var option = createOptionWithProductId(1L, 10000, 10);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));

        assertThatThrownBy(() -> orderCommandService.createOrder(member, 1L, 1, "메시지"))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("카카오 토큰이 없으면 메시지를 보내지 않는다")
    void createOrder_NoKakaoToken() {
        var member = createMemberWithIdAndPoint(1L, 100000);
        var option = createOptionWithProductId(1L, 10000, 10);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
        given(wishRepository.findByMemberIdAndProductId(1L, 1L)).willReturn(Optional.empty());

        orderCommandService.createOrder(member, 1L, 1, "메시지");

        then(kakaoMessageClient).should(never()).sendToMe(any(), any(), any());
    }
}
