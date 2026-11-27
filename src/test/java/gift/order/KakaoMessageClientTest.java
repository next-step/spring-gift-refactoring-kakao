package gift.order;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import gift.category.Category;
import gift.option.Option;
import gift.product.Product;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * KakaoMessageClient.send가 카카오로 보내는 요청(주소, 헤더, template_object)을 지금 그대로 고정한다.
 * 초기 데이터 회원에게는 카카오 액세스 토큰이 없어서 인수 테스트가 이 코드를 지나지 않는다.
 * docs/work/07-order-notification-outbox/tasks.md C0 참고.
 */
class KakaoMessageClientTest {

    private static final String SEND_URL = "https://kapi.kakao.com/v2/api/talk/memo/default/send";
    private static final String KAKAO_ACCESS_TOKEN = "test-kakao-token";

    // 상품 "맥북" 3,360,000원, 옵션 "스페이스 블랙", 주문 수량 2 → 금액 6,720,000원
    private static final String TEMPLATE_WITH_MESSAGE = """
            {
                "object_type": "text",
                "text": "🎁 선물이 도착했어요!\\n\\n맥북 (스페이스 블랙)\\n수량: 2개\\n금액: 6,720,000원\\n\\n💌 생일 축하해",
                "link": {},
                "button_title": "선물 확인하기"
            }
            """;

    private static final String TEMPLATE_WITHOUT_MESSAGE = """
            {
                "object_type": "text",
                "text": "🎁 선물이 도착했어요!\\n\\n맥북 (스페이스 블랙)\\n수량: 2개\\n금액: 6,720,000원",
                "link": {},
                "button_title": "선물 확인하기"
            }
            """;

    private MockRestServiceServer server;
    private KakaoMessageClient kakaoMessageClient;
    private Product product;
    private Option option;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        kakaoMessageClient = new KakaoMessageClient(builder);

        Category category = new Category("교환권", "#6c95d1", "https://example.com/category.jpg", "설명");
        product = new Product("맥북", 3_360_000, "https://example.com/macbook.jpg", category);
        option = new Option(product, "스페이스 블랙", 10);
    }

    @Test
    void 주문_메시지가_있으면_문구_끝에_주문_메시지가_붙는다() {
        expectRequest(TEMPLATE_WITH_MESSAGE);

        kakaoMessageClient.send(
                KAKAO_ACCESS_TOKEN, OrderNotificationMessage.build(new Order(option, 2L, 2, "생일 축하해"), product));

        server.verify();
    }

    @Test
    void 주문_메시지가_null이면_주문_메시지_줄이_없다() {
        expectRequest(TEMPLATE_WITHOUT_MESSAGE);

        kakaoMessageClient.send(
                KAKAO_ACCESS_TOKEN, OrderNotificationMessage.build(new Order(option, 2L, 2, null), product));

        server.verify();
    }

    @Test
    void 주문_메시지가_공백뿐이면_주문_메시지_줄이_없다() {
        expectRequest(TEMPLATE_WITHOUT_MESSAGE);

        kakaoMessageClient.send(
                KAKAO_ACCESS_TOKEN, OrderNotificationMessage.build(new Order(option, 2L, 2, "   "), product));

        server.verify();
    }

    private void expectRequest(String expectedTemplate) {
        server.expect(requestTo(SEND_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + KAKAO_ACCESS_TOKEN))
                .andExpect(content().formDataContains(Map.of("template_object", expectedTemplate)))
                .andRespond(withSuccess());
    }
}
