package gift.order;

import gift.member.Member;
import gift.option.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class KakaoMessageEventListener {
    private static final Logger log = LoggerFactory.getLogger(KakaoMessageEventListener.class);

    private final KakaoMessageClient kakaoMessageClient;

    @Autowired
    public KakaoMessageEventListener(KakaoMessageClient kakaoMessageClient) {
        this.kakaoMessageClient = kakaoMessageClient;
    }

    @TransactionalEventListener
    public void handleOrderCompleted(OrderCompletedEvent event) {
        Member member = event.member();
        Order order = event.order();
        Option option = event.option();

        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            var product = option.getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception e) {
            log.warn("카카오 메시지 전송 실패: memberId={}, orderId={}", member.getId(), order.getId(), e);
        }
    }
}
