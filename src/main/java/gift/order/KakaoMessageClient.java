package gift.order;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoMessageClient implements OrderNotificationSender {
    private final RestClient restClient;

    public KakaoMessageClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    // 카카오톡 "나에게 보내기" API로 보낸다.
    @Override
    public void send(String kakaoAccessToken, String templateObject) {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("template_object", templateObject);

        restClient
                .post()
                .uri("https://kapi.kakao.com/v2/api/talk/memo/default/send")
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(params)
                .retrieve()
                .toBodilessEntity();
    }
}
