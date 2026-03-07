package gift.order.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import gift.global.NotFoundException;
import gift.member.MemberQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KakaoMessagingServiceTest {

    @InjectMocks
    KakaoMessagingService kakaoMessagingService;

    @Mock
    KakaoMessageClient kakaoMessageClient;

    @Mock
    MemberQueryPort memberQueryPort;

    @Test
    @DisplayName("카카오 메시지를 전송한다 — 토큰 조회 + 메시지 전송 순서")
    void testSendDefaultTemplateMessageTo() {
        // given
        Long memberId = 1L;
        String kakaoAccessToken = "test-kakao-token";
        String message = "주문 완료 메시지";
        OrderMessageDto orderMessageDto = new OrderMessageDto(message);

        given(memberQueryPort.getKakaoAccessToken(memberId))
                .willReturn(kakaoAccessToken);

        // when
        kakaoMessagingService.sendDefaultTemplateMessageTo(memberId, orderMessageDto);

        // then
        InOrder inOrder = Mockito.inOrder(memberQueryPort, kakaoMessageClient);
        inOrder.verify(memberQueryPort).getKakaoAccessToken(memberId);
        inOrder.verify(kakaoMessageClient)
                .sendDefaultTemplateMessageToMe(kakaoAccessToken, message);
    }

    @Test
    @DisplayName("사용자가 없으면 NotFoundException 이 전파된다")
    void testSendMessageMemberNotFound() {
        // given
        Long memberId = 999L;
        OrderMessageDto orderMessageDto = new OrderMessageDto("메시지");

        willThrow(NotFoundException.memberNotFound())
                .given(memberQueryPort).getKakaoAccessToken(memberId);

        // when + then
        assertThatThrownBy(
                () -> kakaoMessagingService.sendDefaultTemplateMessageTo(memberId, orderMessageDto))
                .isInstanceOf(NotFoundException.class);

        then(kakaoMessageClient).shouldHaveNoInteractions();
    }
}
