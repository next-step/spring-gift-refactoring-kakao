package gift.order;

import gift.infrastructure.kakao.KakaoMessageClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderCompletedEventListener {
  private final KakaoMessageClient kakaoMessageClient;

  public OrderCompletedEventListener(KakaoMessageClient kakaoMessageClient) {
    this.kakaoMessageClient = kakaoMessageClient;
  }

  @TransactionalEventListener
  public void handle(OrderCompletedEvent event) {
    kakaoMessageClient.send(event.member(), event.order(), event.option());
  }
}
