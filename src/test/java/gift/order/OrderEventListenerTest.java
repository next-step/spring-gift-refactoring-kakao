package gift.order;

import gift.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.Level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private KakaoMessageClient kakaoMessageClient;

    @InjectMocks
    private OrderEventListener listener;

    @Test
    void handleOrderPlaced_nullToken_skipsMessageSend() {
        var order = TestFixtures.order(1L, TestFixtures.option(), 1L, 1, "msg");
        var event = new OrderPlacedEvent(order, null, TestFixtures.product());

        listener.handleOrderPlaced(event);

        then(kakaoMessageClient).should(never()).sendToMe(any(), any(), any());
    }

    @Test
    void handleOrderPlaced_withToken_sendsMessage() {
        var order = TestFixtures.order(1L, TestFixtures.option(), 1L, 1, "msg");
        var product = TestFixtures.product();
        var event = new OrderPlacedEvent(order, "kakao-token", product);

        listener.handleOrderPlaced(event);

        then(kakaoMessageClient).should().sendToMe("kakao-token", order, product);
    }

    @Test
    void handleOrderPlaced_sendFailure_doesNotPropagate() {
        var order = TestFixtures.order(1L, TestFixtures.option(), 1L, 1, "msg");
        var product = TestFixtures.product();
        var event = new OrderPlacedEvent(order, "kakao-token", product);
        doThrow(new RuntimeException("카카오 API 오류"))
            .when(kakaoMessageClient).sendToMe("kakao-token", order, product);

        listener.handleOrderPlaced(event);

        // 예외가 전파되지 않으면 성공
    }

    @Test
    void handleOrderPlaced_sendFailure_logsWarning() {
        var loggerUnderTest = (Logger) LoggerFactory.getLogger(OrderEventListener.class);
        var listAppender = new ListAppender<ILoggingEvent>();
        listAppender.start();
        loggerUnderTest.addAppender(listAppender);

        var order = TestFixtures.order(1L, TestFixtures.option(), 1L, 1, "msg");
        var product = TestFixtures.product();
        var event = new OrderPlacedEvent(order, "kakao-token", product);
        doThrow(new RuntimeException("카카오 API 오류"))
            .when(kakaoMessageClient).sendToMe("kakao-token", order, product);

        listener.handleOrderPlaced(event);

        assertThat(listAppender.list)
            .anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().contains("카카오"));
    }
}
