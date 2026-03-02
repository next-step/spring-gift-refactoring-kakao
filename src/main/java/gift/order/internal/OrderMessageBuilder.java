package gift.order.internal;

import gift.global.NotFoundException;
import gift.option.Option;
import gift.order.Order;
import gift.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMessageBuilder {

    private final OrderRepository orderRepo;

    public OrderMessageDto buildFrom(Long orderId) {

        Order find = orderRepo.findByIdInnerJoinFetchOptionAndProduct(orderId)
                .orElseThrow(NotFoundException::orderNotFound);

        Option option = find.getOption();
        Product product = option.getProduct();

        String productName = product.getName();
        String optionName = option.getName();
        int orderQuantity = find.getQuantity();
        int totalPrice = product.getPrice() * orderQuantity;
        String message = find.getMessage();

        String templateMessage = buildTemplate(
                productName, optionName,
                orderQuantity, totalPrice, message
        );

        return new OrderMessageDto(templateMessage);
    }

    private static String buildTemplate(
            String productName, String optionName,
            int orderQuantity, int totalPrice, String message
    ) {
        return """
                {
                    "object_type": "text",
                    "text": "🎁 선물이 도착했어요!\\n\\n%s (%s)\\n수량: %,d개\\n금액: %,d원\\n%s",
                    "link": {},
                    "button_title": "선물 확인하기"
                }
                """.formatted(
                productName,
                optionName,
                orderQuantity,
                totalPrice,
                message
        );
    }
}
