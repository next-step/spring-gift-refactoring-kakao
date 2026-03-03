package gift.order.internal;

import gift.global.NotFoundException;
import gift.option.Option;
import gift.order.Order;
import gift.product.Product;
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
    private final OrderOptionRepository optionRepo;
    private final OrderMemberRepository memberRepo;

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

        // validate option
        Option option = optionRepo.findByIdInnerJoinFetchProduct(optionId)
                .orElseThrow(NotFoundException::optionNotFound);

        // subtract stock
        option.subtractQuantity(quantity);

        Product product = option.getProduct();

        // deduct points
        int price = product.getPrice() * quantity;
        memberRepo.findById(memberId)
                .orElseThrow(NotFoundException::memberNotFound)
                .deductPoint(price);

        // save order
        Order build = Order.builder()
                .option(option)
                .memberId(memberId)
                .quantity(quantity)
                .message(message)
                .build();

        Order newEntity = orderRepo.save(build);

        return OrderResponse.from(newEntity);
    }
}
