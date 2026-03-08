package gift.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoMessageClient implements OrderMessageClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public KakaoMessageClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendToMe(OrderCompletedEvent event) {
        String templateObject = buildTemplate(event);

        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("template_object", templateObject);

        restClient
                .post()
                .uri("https://kapi.kakao.com/v2/api/talk/memo/default/send")
                .header("Authorization", "Bearer " + event.accessToken())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(params)
                .retrieve()
                .toBodilessEntity();
    }

    private String buildTemplate(OrderCompletedEvent event) {
        String totalPrice = String.format("%,d", event.totalPrice());
        String messageSuffix = event.message() != null && !event.message().isBlank() ? "\n\n💌 " + event.message() : "";
        String text = "🎁 선물이 도착했어요!\n\n%s (%s)\n수량: %d개\n금액: %s원%s"
                .formatted(event.productName(), event.optionName(), event.quantity(), totalPrice, messageSuffix);

        try {
            return objectMapper.writeValueAsString(
                    Map.of("object_type", "text", "text", text, "link", Map.of(), "button_title", "선물 확인하기"));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("카카오 메시지 템플릿 직렬화 실패", e);
        }
    }
}
