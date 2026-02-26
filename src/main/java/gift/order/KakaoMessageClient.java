package gift.order;

import gift.product.Product;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
/**
 * Kakao OAuth2 로그인 흐름을 처리합니다.
 * 1. /login은 사용자를 Kakao의 인증 페이지로 리다이렉트합니다.
 * 2. /callback은 인가 코드를 받아 액세스 토큰으로 교환하고,
 *    사용자 정보를 조회한 뒤, 신규 회원이면 자동 가입시키고 서비스 JWT를 발급합니다.
 */
public class KakaoMessageClient {
    private final RestClient restClient;

    public KakaoMessageClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public void sendToMe(String accessToken, Order order, Product product) {
        var templateObject = buildTemplate(order, product);

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
