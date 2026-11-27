package gift.acceptance;

import gift.order.OrderNotificationSender;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 인수 테스트에서 카카오 대신 주문 알림을 받는 가짜 발송기.
 * 받은 (카카오 액세스 토큰, 문구)를 순서대로 기록한다. 실패한 발송도 받은 것으로 기록한다.
 * 시나리오마다 OrderNotificationSteps의 @Before가 reset()을 부른다.
 */
@Component
@Primary
public class FakeOrderNotificationSender implements OrderNotificationSender {

    public static final String FAILURE_MESSAGE = "가짜 카카오 발송 실패";

    public record Received(String kakaoAccessToken, String message) {}

    private final List<Received> received = new ArrayList<>();
    private int failuresLeft;
    private boolean failAlways;

    @Override
    public synchronized void send(String kakaoAccessToken, String message) {
        received.add(new Received(kakaoAccessToken, message));
        if (failAlways) {
            throw new IllegalStateException(FAILURE_MESSAGE);
        }
        if (failuresLeft > 0) {
            failuresLeft--;
            throw new IllegalStateException(FAILURE_MESSAGE);
        }
    }

    public synchronized void failNext(int times) {
        this.failuresLeft = times;
    }

    public synchronized void failAlways() {
        this.failAlways = true;
    }

    public synchronized List<Received> received() {
        return List.copyOf(received);
    }

    public synchronized void reset() {
        received.clear();
        failuresLeft = 0;
        failAlways = false;
    }
}
