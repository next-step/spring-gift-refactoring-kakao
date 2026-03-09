package gift.order;

import gift.ServiceTestFixture;
import gift.wish.Wish;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RecordApplicationEvents
class OrderServiceTest extends ServiceTestFixture {

    @Autowired
    OrderService orderService;

    @Autowired
    ApplicationEvents events;

    @Test
    void 주문_성공_시_재고가_차감된다() {
        var option = createOption(1000, 50);
        var member = createMember("stock@test.com", 10000);

        orderService.createOrder(member, new OrderRequest(option.getId(), 3, "테스트"));

        var updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(47);
    }

    @Test
    void 주문_성공_시_포인트가_차감된다() {
        var option = createOption(1000, 50);
        var member = createMember("point@test.com", 10000);

        orderService.createOrder(member, new OrderRequest(option.getId(), 3, "테스트"));

        var updated = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(updated.getPoint()).isEqualTo(7000);
    }

    @Test
    void 주문_성공_시_위시리스트가_자동_삭제된다() {
        var option = createOption(1000, 50);
        var member = createMember("wish@test.com", 10000);
        wishRepository.save(new Wish(member.getId(), option.getProduct()));

        orderService.createOrder(member, new OrderRequest(option.getId(), 1, "테스트"));

        assertThat(wishRepository.findByMemberIdAndProductId(member.getId(), option.getProduct().getId()))
            .isEmpty();
    }

    @Test
    void 카카오_토큰이_있으면_OrderCompletedEvent가_발행된다() {
        var option = createOption(1000, 50);
        var member = createKakaoMember("kakao@test.com", 10000);

        orderService.createOrder(member, new OrderRequest(option.getId(), 1, "테스트"));

        assertThat(events.stream(OrderCompletedEvent.class).count()).isEqualTo(1);
    }

    @Test
    void 카카오_토큰이_없으면_이벤트가_발행되지_않는다() {
        var option = createOption(1000, 50);
        var member = createMember("nokakao@test.com", 10000);

        orderService.createOrder(member, new OrderRequest(option.getId(), 1, "테스트"));

        assertThat(events.stream(OrderCompletedEvent.class).count()).isZero();
    }

    @Test
    void 재고_부족_시_예외가_발생하고_롤백된다() {
        var option = createOption(1000, 5);
        var member = createMember("nostock@test.com", 100000);
        int beforePoint = member.getPoint();

        assertThatThrownBy(() ->
            orderService.createOrder(member, new OrderRequest(option.getId(), 10, "테스트"))
        ).isInstanceOf(IllegalArgumentException.class);

        assertThat(optionRepository.findById(option.getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getPoint()).isEqualTo(beforePoint);
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void 포인트_부족_시_예외가_발생하고_롤백된다() {
        var option = createOption(1000, 50);
        var member = createMember("poor@test.com", 500);

        assertThatThrownBy(() ->
            orderService.createOrder(member, new OrderRequest(option.getId(), 1, "테스트"))
        ).isInstanceOf(IllegalArgumentException.class);

        assertThat(optionRepository.findById(option.getId()).orElseThrow().getQuantity()).isEqualTo(50);
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getPoint()).isEqualTo(500);
        assertThat(orderRepository.count()).isZero();
    }
}
