package gift.order.internal;

import gift.member.MemberCommandPort;
import gift.option.Option;
import gift.option.OptionCommandPort;
import gift.option.OptionQueryPort;
import gift.order.Order;
import gift.product.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OptionQueryPort optionQueryPort;
    private final OptionCommandPort optionCommandPort;
    private final MemberCommandPort memberCommandPort;

    public PagedModel<OrderResponse> getOrders(Long memberId, Pageable pageable) {
        Page<OrderResponse> pageResponse = orderRepo.findByMemberId(memberId, pageable)
                .map(OrderResponse::from);

        return new PagedModel<>(pageResponse);
    }

    @Transactional
    public OrderResponse createOrder(Long memberId, OrderRequest request) {
        Long optionId = request.optionId();
        int quantity = request.quantity();
        String message = request.message();

        // subtract stock
        optionCommandPort.subtractQuantity(optionId, quantity);

        // deduct points
        ProductDto product = optionQueryPort.getAssociatedProduct(optionId);
        int price = product.price() * quantity;
        memberCommandPort.deductPoint(memberId, price);

        // save order
        Option optionRef = optionQueryPort.getReference(optionId);
        Order build = Order.builder()
                .option(optionRef)
                .memberId(memberId)
                .quantity(quantity)
                .message(message)
                .build();

        Order newEntity = orderRepo.save(build);

        return OrderResponse.from(newEntity);
    }
}
