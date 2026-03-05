package gift.order;

import gift.TestFixtures;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
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
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private Member member;
    private Option option;

    @BeforeEach
    void setUp() {
        member = TestFixtures.member(1L, "test@test.com", "password");
        member.chargePoint(100000);
        option = TestFixtures.option(1L, TestFixtures.product(), "기본 옵션", 100);
    }

    @Test
    void placeOrder_happyPath_completesAllSteps() {
        var request = new OrderRequest(1L, 2, "선물입니다");
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        var result = orderService.placeOrder(member, request);

        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(2);
        then(optionRepository).should().save(option);
        then(memberRepository).should().save(member);
        then(orderRepository).should().save(any(Order.class));
    }

    @Test
    void placeOrder_optionNotFound_throws() {
        var request = new OrderRequest(99L, 1, null);
        given(optionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(member, request))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void placeOrder_insufficientStock_throws() {
        var request = new OrderRequest(1L, 999, null);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));

        assertThatThrownBy(() -> orderService.placeOrder(member, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("재고");
    }

    @Test
    void placeOrder_insufficientPoints_throws() {
        var poorMember = TestFixtures.member(2L, "poor@test.com", "pw");
        var request = new OrderRequest(1L, 1, null);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));

        assertThatThrownBy(() -> orderService.placeOrder(poorMember, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("포인트");
    }

    @Test
    void placeOrder_savesOrderWithCorrectFields() {
        var request = new OrderRequest(1L, 3, "축하합니다");
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        var result = orderService.placeOrder(member, request);

        assertThat(result.getOption()).isEqualTo(option);
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getQuantity()).isEqualTo(3);
        assertThat(result.getMessage()).isEqualTo("축하합니다");
    }

    @Test
    void placeOrder_calculatesCorrectPrice() {
        var request = new OrderRequest(1L, 5, null);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(member, request);

        // price = 10000 * 5 = 50000, 초기 100000 - 50000 = 50000
        assertThat(member.getPoint()).isEqualTo(50000);
    }

    @Test
    void placeOrder_publishesOrderPlacedEvent() {
        var kakaoMember = TestFixtures.kakaoMember(1L, "kakao@test.com", "kakao-token");
        kakaoMember.chargePoint(100000);
        var request = new OrderRequest(1L, 1, null);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(kakaoMember, request);

        var captor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.kakaoAccessToken()).isEqualTo("kakao-token");
        assertThat(event.product()).isEqualTo(option.getProduct());
    }

    @Test
    void placeOrder_withoutKakaoToken_publishesEventWithNullToken() {
        var request = new OrderRequest(1L, 1, null);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(member, request);

        var captor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        assertThat(captor.getValue().kakaoAccessToken()).isNull();
    }

    @Test
    void findByMemberId_returnsPagedOrders() {
        var order = TestFixtures.order(1L, option, 1L, 2, "메시지");
        var pageable = PageRequest.of(0, 10);
        given(orderRepository.findByMemberId(1L, pageable)).willReturn(new PageImpl<>(List.of(order)));

        var result = orderService.findByMemberId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }
}
