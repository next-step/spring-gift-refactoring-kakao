package gift.order;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoMessageClient {
    private final RestClient restClient;

    public KakaoMessageClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public void sendToMe(String accessToken, KakaoOrderMessage orderMessage) {
        var templateObject = buildTemplate(orderMessage);

        var params = new LinkedMultiValueMap<String, String>();
        params.add("template_object", templateObject);

        restClient.post()
            .uri("https://kapi.kakao.com/v2/api/talk/memo/default/send")
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(params)
            .retrieve()
            .toBodilessEntity();
    }

    private String buildTemplate(KakaoOrderMessage orderMessage) {
        var formattedPrice = String.format("%,d", orderMessage.totalPrice());
        var messageText = orderMessage.message() != null && !orderMessage.message().isBlank()
            ? "\\n\\n💌 " + orderMessage.message()
            : "";
        return """
            {
                "object_type": "text",
                "text": "🎁 선물이 도착했어요!\\n\\n%s (%s)\\n수량: %d개\\n금액: %s원%s",
                "link": {},
                "button_title": "선물 확인하기"
            }
            """.formatted(
            orderMessage.productName(),
            orderMessage.optionName(),
            orderMessage.quantity(),
            formattedPrice,
            messageText
        );
    }
}
