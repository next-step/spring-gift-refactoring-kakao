package gift.infrastructure.kakao;

import gift.auth.AuthConstants;
import gift.order.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoMessageClient {
  private final KakaoMessageProperties properties;
  private final RestClient restClient;

  public KakaoMessageClient(KakaoMessageProperties properties, RestClient.Builder builder) {
    this.properties = properties;
    this.restClient = builder.build();
  }

  public void sendToMe(String accessToken, Order order) {
    String templateObject = buildTemplate(order);

    LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("template_object", templateObject);

    restClient
        .post()
        .uri(properties.sendUrl())
        .header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + accessToken)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .body(params)
        .retrieve()
        .toBodilessEntity();
  }

  private String buildTemplate(Order order) {
    String productName = order.getOption().getProduct().getName();
    String optionName = order.getOption().getName();
    int totalPrice = order.getOption().calculateTotalPrice(order.getQuantity());
    String formattedPrice = String.format("%,d", totalPrice);
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
        .formatted(productName, optionName, order.getQuantity(), formattedPrice, message);
  }
}
