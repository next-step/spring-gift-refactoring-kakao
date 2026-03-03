package gift.order.internal;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoMessageClient {

    private final RestClient restClient;

    public KakaoMessageClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public void sendDefaultTemplateMessageToMe(String kakaoAccessToken, String templateObject) {

        LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("template_object", templateObject);

        restClient.post()
                .uri("https://kapi.kakao.com/v2/api/talk/memo/default/send")
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(parameters)
                .retrieve()
                .toBodilessEntity();
    }
}
