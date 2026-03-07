package gift.order.internal;

import gift.member.MemberQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoMessagingService {

    private final KakaoMessageClient kakaoMessageClient;
    private final MemberQueryPort memberQueryPort;

    public void sendDefaultTemplateMessageTo(Long memberId, OrderMessageDto orderMessageDto) {
        String kakaoAccessToken = memberQueryPort.getKakaoAccessToken(memberId);

        String templateObject = orderMessageDto.message();

        kakaoMessageClient.sendDefaultTemplateMessageToMe(
                kakaoAccessToken, templateObject
        );
    }
}
