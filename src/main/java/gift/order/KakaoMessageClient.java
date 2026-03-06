package gift.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gift.option.Option;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoMessageClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public KakaoMessageClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
    }

    public void sendToMe(String accessToken, Order order, Option option) {
        var templateObject = buildTemplate(order, option);

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

    // 생성되는 JSON 예시:
    // {
    //   "object_type": "text",
    //   "text": "🎁 선물이 도착했어요!\n\n상품명 (옵션명)\n수량: 3개\n금액: 3,000원\n\n💌 메시지",
    //   "link": {},
    //   "button_title": "선물 확인하기"
    // }
    private String buildTemplate(Order order, Option option) {
        var totalPrice = String.format("%,d", option.calculateTotalPrice(order.getQuantity()));
        var message = order.getMessage() != null && !order.getMessage().isBlank()
            ? "\n\n\uD83D\uDC8C " + order.getMessage()
            : "";
        var text = "\uD83C\uDF81 선물이 도착했어요!\n\n%s (%s)\n수량: %d개\n금액: %s원%s".formatted(
            option.getProductName(),
            option.getName(),
            order.getQuantity(),
            totalPrice,
            message
        );

        ObjectNode node = objectMapper.createObjectNode();
        node.put("object_type", "text");
        node.put("text", text);
        node.putObject("link");
        node.put("button_title", "선물 확인하기");
        return node.toString();
    }
}
