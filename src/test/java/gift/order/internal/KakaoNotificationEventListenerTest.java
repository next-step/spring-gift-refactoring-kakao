package gift.order.internal;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import gift.order.OrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KakaoNotificationEventListenerTest {

    @InjectMocks
    KakaoNotificationEventListener listener;

    @Mock
    OrderMessageBuilder orderMessageBuilder;

    @Mock
    KakaoMessagingService kakaoMessagingService;

    @Test
    @DisplayName("주문 이벤트 수신 시 메시지를 빌드하고 카카오 알림을 전송한다")
    void testOnOrderCreated() {
        // given
        Long memberId = 1L;
        Long productId = 10L;
        Long orderId = 100L;
        OrderCreatedEvent event = new OrderCreatedEvent(memberId, productId, orderId);

        OrderMessageDto dto = new OrderMessageDto("template");
        given(orderMessageBuilder.buildFrom(orderId))
                .willReturn(dto);

        // when
        listener.onOrderCreated(event);

        // then
        then(orderMessageBuilder).should().buildFrom(orderId);
        then(kakaoMessagingService).should()
                .sendDefaultTemplateMessageTo(memberId, dto);
    }

    @Test
    @DisplayName("메시지 빌드 실패 시 예외를 삼키고 알림을 전송하지 않는다")
    void testOnOrderCreatedBuildFailure() {
        // given
        Long memberId = 1L;
        Long productId = 10L;
        Long orderId = 100L;
        OrderCreatedEvent event = new OrderCreatedEvent(memberId, productId, orderId);

        given(orderMessageBuilder.buildFrom(orderId))
                .willThrow(new RuntimeException("build failed"));

        // when
        listener.onOrderCreated(event);

        // then
        then(kakaoMessagingService).should(never())
                .sendDefaultTemplateMessageTo(memberId, new OrderMessageDto("template"));
    }
}
