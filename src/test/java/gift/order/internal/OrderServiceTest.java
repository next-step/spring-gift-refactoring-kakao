package gift.order.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import gift.global.NotFoundException;
import gift.member.MemberCommandPort;
import gift.option.Option;
import gift.option.OptionCommandPort;
import gift.option.OptionQueryPort;
import gift.order.Order;
import gift.order.OrderCreatedEvent;
import gift.product.ProductDto;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PagedModel.PageMetadata;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    OrderService orderService;

    @Mock
    OrderRepository orderRepo;

    @Mock
    OptionQueryPort optionQueryPort;

    @Mock
    OptionCommandPort optionCommandPort;

    @Mock
    MemberCommandPort memberCommandPort;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("주문 목록을 조회한다 — orderRepo.findByMemberId 호출 + PagedModel 응답 매핑")
    void testGetOrders() {
        // given
        Long memberId = 1L;
        int pageNumber = 0;
        Pageable pageable = PageRequest.of(pageNumber, 10);

        Option option = createOption(5L);
        Order order = createOrder(100L, option, memberId, 2, "메시지");

        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);
        given(orderRepo.findByMemberId(memberId, pageable))
                .willReturn(page);

        // when
        PagedModel<OrderResponse> response = orderService.getOrders(memberId, pageable);

        // then
        then(orderRepo).should()
                .findByMemberId(memberId, pageable);

        assertThat(response.getContent()).hasSize(1);

        PageMetadata metadata = response.getMetadata();
        assertThat(metadata.size()).isEqualTo(10);
        assertThat(metadata.number()).isEqualTo(pageNumber);
        assertThat(metadata.totalElements()).isOne();
        assertThat(metadata.totalPages()).isOne();

        OrderResponse content = response.getContent().getFirst();
        assertThat(content.id()).isEqualTo(order.getId());
        assertThat(content.optionId()).isEqualTo(option.getId());
        assertThat(content.quantity()).isEqualTo(order.getQuantity());
        assertThat(content.message()).isEqualTo(order.getMessage());
    }

    private static Option createOption(Long id) {
        Option option = Option.builder()
                .name("옵션")
                .quantity(100)
                .build();

        setId(option, Option.class, id);
        return option;
    }

    @SuppressWarnings("SameParameterValue")
    private static Order createOrder(Long id, Option option, Long memberId, int quantity,
            String message) {
        Order order = Order.builder()
                .option(option)
                .memberId(memberId)
                .quantity(quantity)
                .message(message)
                .build();

        setId(order, Order.class, id);
        return order;
    }

    private static <T> void setId(T entity, Class<T> clazz, Long id) {
        try {
            Field idField = clazz.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    @DisplayName("주문을 생성한다 — 재고 차감 + 포인트 차감 + 주문 저장 + 응답 매핑")
    void testCreateOrder() {
        // given
        Long memberId = 1L;
        Long optionId = 5L;
        int quantity = 3;
        String message = "선물 메시지";
        int unitPrice = 1000;

        ProductDto productDto = new ProductDto(10L, "상품", unitPrice, "https://img.png", 1L);
        Option optionRef = createOption(optionId);
        Order savedOrder = createOrder(100L, optionRef, memberId, quantity, message);

        given(optionQueryPort.getAssociatedProduct(optionId))
                .willReturn(productDto);
        given(optionQueryPort.getReference(optionId))
                .willReturn(optionRef);
        given(orderRepo.save(any(Order.class)))
                .willReturn(savedOrder);

        // when
        OrderResponse response = orderService.createOrder(memberId,
                new OrderRequest(optionId, quantity, message));

        // then — 호출 여부 + 인자 검증
        then(optionCommandPort).should().subtractQuantity(optionId, quantity);
        then(optionQueryPort).should().getAssociatedProduct(optionId);
        then(memberCommandPort).should().deductPoint(memberId, unitPrice * quantity);
        then(optionQueryPort).should().getReference(optionId);
        then(orderRepo).should().save(any(Order.class));
        then(eventPublisher).should().publishEvent(new OrderCreatedEvent(
                memberId, productDto.id(), savedOrder.getId()
        ));

        // then — 응답 매핑
        assertThat(response.id()).isEqualTo(savedOrder.getId());
        assertThat(response.optionId()).isEqualTo(optionId);
        assertThat(response.quantity()).isEqualTo(quantity);
        assertThat(response.message()).isEqualTo(message);
    }

    @Test
    @DisplayName("주문 생성 시 옵션이 없으면 NotFoundException 이 전파된다")
    void testCreateOrderOptionNotFound() {
        // given
        Long memberId = 1L;
        Long optionId = 999L;

        willThrow(NotFoundException.optionNotFound())
                .given(optionCommandPort).subtractQuantity(optionId, 1);

        // when + then
        assertThatThrownBy(
                () -> orderService.createOrder(memberId, new OrderRequest(optionId, 1, "msg")))
                .isInstanceOf(NotFoundException.class);
    }

    // -- fixtures --

    @Test
    @DisplayName("주문 생성 시 재고가 부족하면 IllegalArgumentException 이 전파된다")
    void testCreateOrderInsufficientStock() {
        // given
        Long memberId = 1L;
        Long optionId = 5L;
        int quantity = 9999;

        willThrow(new IllegalArgumentException("차감할 수량이 현재 재고보다 많습니다."))
                .given(optionCommandPort).subtractQuantity(optionId, quantity);

        // when + then
        assertThatThrownBy(() -> orderService.createOrder(memberId,
                new OrderRequest(optionId, quantity, "msg")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("주문 생성 시 사용자가 없으면 NotFoundException 이 전파된다")
    void testCreateOrderMemberNotFound() {
        // given
        Long memberId = 999L;
        Long optionId = 5L;
        int quantity = 1;
        int unitPrice = 1000;

        ProductDto productDto = new ProductDto(10L, "상품", unitPrice, "https://img.png", 1L);
        given(optionQueryPort.getAssociatedProduct(optionId))
                .willReturn(productDto);

        willThrow(NotFoundException.memberNotFound())
                .given(memberCommandPort).deductPoint(memberId, unitPrice * quantity);

        // when + then
        assertThatThrownBy(() -> orderService.createOrder(memberId,
                new OrderRequest(optionId, quantity, "msg")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("주문 생성 시 포인트가 부족하면 IllegalArgumentException 이 전파된다")
    void testCreateOrderInsufficientPoints() {
        // given
        Long memberId = 1L;
        Long optionId = 5L;
        int quantity = 1;
        int unitPrice = 1000;

        ProductDto productDto = new ProductDto(10L, "상품", unitPrice, "https://img.png", 1L);
        given(optionQueryPort.getAssociatedProduct(optionId))
                .willReturn(productDto);

        willThrow(new IllegalArgumentException("포인트가 부족합니다."))
                .given(memberCommandPort).deductPoint(memberId, unitPrice * quantity);

        // when + then
        assertThatThrownBy(() -> orderService.createOrder(memberId,
                new OrderRequest(optionId, quantity, "msg")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
