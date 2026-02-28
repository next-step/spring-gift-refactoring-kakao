package gift.order;

import gift.category.Category;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
    private Category category;
    private Product product;
    private Option option;

    @BeforeEach
    void setUp() throws Exception {
        category = new Category("교환권", "#ffffff", "img.png", "");
        setId(category, 1L);

        product = new Product("스타벅스 아메리카노", 5000, "img.png", category);
        setId(product, 1L);

        option = new Option(product, "Tall", 100);
        setId(option, 1L);

        member = new Member("test@test.com", "password");
        setId(member, 1L);
        member.chargePoint(100000);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("정상적으로 주문을 생성한다")
        void createsOrderSuccessfully() throws Exception {
            var request = new OrderRequest(1L, 2, "선물입니다");
            var savedOrder = new Order(option, member.getId(), 2, "선물입니다");
            setId(savedOrder, 1L);

            given(optionRepository.findById(1L)).willReturn(Optional.of(option));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

            Order result = orderService.createOrder(member, request);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getQuantity()).isEqualTo(2);
            assertThat(result.getMessage()).isEqualTo("선물입니다");
        }

        @Test
        @DisplayName("주문 시 재고가 차감된다")
        void subtractsStock() {
            var request = new OrderRequest(1L, 3, "");

            given(optionRepository.findById(1L)).willReturn(Optional.of(option));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            orderService.createOrder(member, request);

            assertThat(option.getQuantity()).isEqualTo(97);
            then(optionRepository).should().save(option);
        }

        @Test
        @DisplayName("주문 시 포인트가 차감된다")
        void deductsPoints() {
            var request = new OrderRequest(1L, 2, "");

            given(optionRepository.findById(1L)).willReturn(Optional.of(option));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            orderService.createOrder(member, request);

            assertThat(member.getPoint()).isEqualTo(90000); // 100000 - (5000 * 2)
            then(memberRepository).should().save(member);
        }

        @Test
        @DisplayName("존재하지 않는 옵션이면 예외를 던진다")
        void throwsWhenOptionNotFound() {
            var request = new OrderRequest(999L, 1, "");

            given(optionRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrder(member, request))
                .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("카카오 토큰이 있으면 알림을 전송한다")
        void sendsKakaoMessageWhenTokenExists() {
            member.updateKakaoAccessToken("kakao-token");
            var request = new OrderRequest(1L, 1, "");

            given(optionRepository.findById(1L)).willReturn(Optional.of(option));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            orderService.createOrder(member, request);

            then(kakaoMessageClient).should().sendToMe(any(), any(), any());
        }

        @Test
        @DisplayName("카카오 토큰이 없으면 알림을 전송하지 않는다")
        void skipsKakaoMessageWhenNoToken() {
            var request = new OrderRequest(1L, 1, "");

            given(optionRepository.findById(1L)).willReturn(Optional.of(option));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            orderService.createOrder(member, request);

            then(kakaoMessageClient).should(never()).sendToMe(any(), any(), any());
        }

        @Test
        @DisplayName("카카오 알림 실패해도 주문은 성공한다")
        void orderSucceedsEvenIfKakaoFails() throws Exception {
            member.updateKakaoAccessToken("kakao-token");
            var request = new OrderRequest(1L, 1, "");
            var savedOrder = new Order(option, member.getId(), 1, "");
            setId(savedOrder, 1L);

            given(optionRepository.findById(1L)).willReturn(Optional.of(option));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            org.mockito.BDDMockito.willThrow(new RuntimeException("카카오 API 오류"))
                .given(kakaoMessageClient).sendToMe(any(), any(), any());

            Order result = orderService.createOrder(member, request);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("getOrders")
    class GetOrders {

        @Test
        @DisplayName("회원의 주문 목록을 페이징 조회한다")
        void returnsPagedOrders() throws Exception {
            var order = new Order(option, 1L, 2, "선물");
            setId(order, 1L);
            var pageable = PageRequest.of(0, 10);

            given(orderRepository.findByMemberId(1L, pageable))
                .willReturn(new PageImpl<>(List.of(order)));

            var result = orderService.getOrders(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).quantity()).isEqualTo(2);
        }
    }
}
