package gift.order.internal;

import gift.global.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoMessagingService {

    private final KakaoMessageClient kakaoMessageClient;
    private final OrderMemberRepository memberRepo;

    public void sendDefaultTemplateMessageTo(Long memberId, OrderMessageDto orderMessageDto) {
        String kakaoAccessToken = memberRepo.findById(memberId)
                .orElseThrow(NotFoundException::memberNotFound)
                .getKakaoAccessToken();

        String templateObject = orderMessageDto.message();

        kakaoMessageClient.sendDefaultTemplateMessageToMe(
                kakaoAccessToken, templateObject
        );
    }
}
