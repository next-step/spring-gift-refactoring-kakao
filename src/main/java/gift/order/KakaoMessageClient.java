package gift.order;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import gift.product.Product;

@Component
public class KakaoMessageClient implements MessageClient {
    private final RestClient restClient;

    public KakaoMessageClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    @Override
    public void sendToMe(String accessToken, Order order, Product product) {
        String templateObject = buildTemplate(order, product);

        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("template_object", templateObject);

        restClient.post()
            .uri("https://kapi.kakao.com/v2/api/talk/memo/default/send")
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(params)
            .retrieve()
            .toBodilessEntity();
    }

    private String buildTemplate(Order order, Product product) {
        String totalPrice = String.format("%,d", product.getPrice() * order.getQuantity());
        String message = order.hasMessage()
            ? "\\n\\n💌 " + order.getMessage()
            : "";
        return """
            {
                "object_type": "text",
                "text": "🎁 선물이 도착했어요!\\n\\n%s (%s)\\n수량: %d개\\n금액: %s원%s",
                "link": {},
                "button_title": "선물 확인하기"
            }
            """.formatted(
            product.getName(),
            order.getOption().getName(),
            order.getQuantity(),
            totalPrice,
            message
        );
    }
}
