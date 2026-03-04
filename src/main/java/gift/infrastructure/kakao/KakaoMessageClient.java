package gift.infrastructure.kakao;

import gift.order.Order;
import gift.product.Product;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoMessageClient {
  private static final String BEARER_PREFIX = "Bearer ";

  private final KakaoMessageProperties properties;
  private final RestClient restClient;

  public KakaoMessageClient(KakaoMessageProperties properties, RestClient.Builder builder) {
    this.properties = properties;
    this.restClient = builder.build();
  }

  public void sendToMe(String accessToken, Order order, Product product) {
    String templateObject = buildTemplate(order, product);

    LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("template_object", templateObject);

    restClient
        .post()
        .uri(properties.sendUrl())
        .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .body(params)
        .retrieve()
        .toBodilessEntity();
  }

  private String buildTemplate(Order order, Product product) {
    String totalPrice = String.format("%,d", product.getPrice() * order.getQuantity());
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
