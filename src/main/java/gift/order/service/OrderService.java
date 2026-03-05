package gift.order.service;

import gift.auth.exception.AuthenticationException;
import gift.auth.jwt.AuthenticationResolver;
import gift.external.ExternalProvider;
import gift.message.MessageClient;
import gift.message.MessageClientRegistry;
import gift.member.entity.Member;
import gift.member.repository.MemberRepository;
import gift.option.entity.Option;
import gift.option.repository.OptionRepository;
import gift.order.dto.OrderRequest;
import gift.order.dto.OrderResponse;
import gift.order.entity.Order;
import gift.order.exception.OrderErrorCode;
import gift.order.exception.OrderException;
import gift.order.repository.OrderRepository;
import gift.wish.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final WishRepository wishRepository;
    private final AuthenticationResolver authenticationResolver;
    private final MessageClientRegistry messageClientRegistry;

    public Page<OrderResponse> getOrders(String authorization, Pageable pageable) {
        Member member = extractMember(authorization);
        return orderRepository.findByMemberId(member.getId(), pageable).map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse createOrder(String authorization, OrderRequest request) {
        Member member = extractMember(authorization);

        Option option = optionRepository.findById(request.optionId())
            .orElseThrow(() -> new OrderException(OrderErrorCode.OPTION_NOT_FOUND));

        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        int price = option.getProduct().getPrice() * request.quantity();
        member.deductPoint(price);
        memberRepository.save(member);

        Order order = new Order(option, member.getId(), request.quantity(), request.message());
        Order saved = orderRepository.save(order);

        wishRepository.deleteByMemberIdAndProductId(member.getId(), option.getProduct().getId());

        sendKakaoMessageIfPossible(member, saved, option);

        return OrderResponse.from(saved);
    }

    private void sendKakaoMessageIfPossible(Member member, Order order, Option option) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            MessageClient messageClient = messageClientRegistry.get(ExternalProvider.KAKAO);
            messageClient.sendToMe(member.getKakaoAccessToken(), order, option.getProduct());
        } catch (Exception ignored) {
        }
    }

    private Member extractMember(String authorization) {
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            throw new AuthenticationException();
        }
        return member;
    }
}
