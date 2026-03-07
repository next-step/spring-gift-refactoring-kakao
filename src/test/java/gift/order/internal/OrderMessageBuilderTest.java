package gift.order.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import gift.global.NotFoundException;
import gift.option.Option;
import gift.order.Order;
import gift.product.Product;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderMessageBuilderTest {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    @InjectMocks
    OrderMessageBuilder orderMessageBuilder;

    @Mock
    OrderRepository orderRepo;

    @Test
    @DisplayName("주문 정보로 카카오 메시지 DTO를 생성한다")
    void testBuildFrom() {
        // given
        Long orderId = 1L;
        Product product = createProduct(10L, "아메리카노", 5000);
        Option option = createOption(100L, product, "TALL", 50);
        Order order = createOrder(orderId, option, 1L, 2, "감사합니다");

        given(orderRepo.findByIdInnerJoinFetchOptionAndProduct(orderId))
                .willReturn(Optional.of(order));

        // when
        OrderMessageDto dto = orderMessageBuilder.buildFrom(orderId);

        // then
        String message = dto.message();
        assertThat(message).contains("아메리카노");
        assertThat(message).contains("TALL");
        assertThat(message).contains("2");
        assertThat(message).contains("10,000");
        assertThat(message).contains("감사합니다");
    }

    @SuppressWarnings("SameParameterValue")
    private static Product createProduct(Long id, String name, int price) {
        Product product = Product.builder()
                .name(name)
                .price(price)
                .imageUrl("https://img.png")
                .build();

        setId(product, Product.class, id);
        return product;
    }

    // -- fixtures --

    @SuppressWarnings("SameParameterValue")
    private static Option createOption(Long id, Product product, String name, int quantity) {
        Option option = Option.builder()
                .product(product)
                .name(name)
                .quantity(quantity)
                .build();

        setId(option, Option.class, id);
        return option;
    }

    @SuppressWarnings("SameParameterValue")
    private static Order createOrder(
            Long id, Option option, Long memberId, int quantity, String message
    ) {
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
    @DisplayName("주문이 존재하지 않으면 NotFoundException을 던진다")
    void testBuildFromOrderNotFound() {
        // given
        given(orderRepo.findByIdInnerJoinFetchOptionAndProduct(NOT_EXISTING_ID))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> orderMessageBuilder.buildFrom(NOT_EXISTING_ID))
                .isInstanceOf(NotFoundException.class);
    }
}
