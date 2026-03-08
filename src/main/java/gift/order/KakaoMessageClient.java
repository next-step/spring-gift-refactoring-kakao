package gift.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class KakaoMessageClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public KakaoMessageClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
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
            ? "\n\n💌 " + orderMessage.message()
            : "";
        var text = "🎁 선물이 도착했어요!\n\n%s (%s)\n수량: %d개\n금액: %s원%s".formatted(
            orderMessage.productName(),
            orderMessage.optionName(),
            orderMessage.quantity(),
            formattedPrice,
            messageText
        );
        var template = Map.of(
            "object_type", "text",
            "text", text,
            "link", Map.of(),
            "button_title", "선물 확인하기"
        );
        try {
            return objectMapper.writeValueAsString(template);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("카카오 메시지 템플릿 직렬화 실패", e);
        }
    }
}
