package gift.infrastructure.kakao;

import gift.member.Member;
import gift.option.Option;
import gift.order.Order;
import gift.product.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class KakaoRestMessageClient implements KakaoMessageClient {
  private static final String BEARER_PREFIX = "Bearer ";

  private final RestClient restClient;

  public KakaoRestMessageClient(RestClient.Builder builder) {
    this.restClient = builder.build();
  }

  @Override
  @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500))
  public void send(Member member, Order order, Option option) {
    if (member.getKakaoAccessToken() == null) {
      return;
    }
    String templateObject = buildTemplate(order, option);

    LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("template_object", templateObject);

    restClient
        .post()
        .uri(KakaoUrls.SEND_MESSAGE)
        .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + member.getKakaoAccessToken())
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .body(params)
        .retrieve()
        .toBodilessEntity();
  }

  @Recover
  public void recover(Exception e, Member member, Order order, Option option) {
    log.error("카카오 메시지 전송 실패 (3회 재시도 소진) orderId={}", order.getId(), e);
    throw new KakaoMessageException("카카오 메시지 전송에 실패했습니다.", e);
  }

  private String buildTemplate(Order order, Option option) {
    Product product = option.getProduct();
    String totalPrice = String.format("%,d", option.calculateTotalPrice(order.getQuantity()));
    String message =
        order.getMessage() != null && !order.getMessage().isBlank()
            ? "\\n\\n💌 " + order.getMessage()
            : "";
    return """
            {
                "object_type": "text",
                "text": "🎁 선물이 도착했어요!\\n\\n%s (%s)\\n수량: %d개\\n금액: %s원%s",
                "link": {},
                "button_title": "선물 확인하기"
            }
            """
        .formatted(
            product.getName(),
            order.getOption().getName(),
            order.getQuantity(),
            totalPrice,
            message);
  }
}
