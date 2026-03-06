package gift.order;

import gift.product.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoMessageClient implements MessageClient {
    private final RestClient restClient;
    private final String messageSendUrl;

    public KakaoMessageClient(
        RestClient.Builder builder,
        @Value("${kakao.api.message-send-url}") String messageSendUrl
    ) {
        this.restClient = builder.build();
        this.messageSendUrl = messageSendUrl;
    }

    public void sendToMe(String accessToken, Order order, Product product) {
        var templateObject = buildTemplate(order, product);

        var params = new LinkedMultiValueMap<String, String>();
        params.add("template_object", templateObject);

        restClient.post()
            .uri(messageSendUrl)
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(params)
            .retrieve()
            .toBodilessEntity();
    }

    private String buildTemplate(Order order, Product product) {
        var totalPrice = String.format("%,d", product.getPrice() * order.getQuantity());
        var message = order.getMessage() != null && !order.getMessage().isBlank()
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
