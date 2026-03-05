package gift.auth.internal;

import gift.global.NotFoundException;
import gift.member.MemberCommandPort;
import gift.member.MemberInfo;
import gift.member.MemberQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final KakaoLoginClient kakaoLoginClient;
    private final MemberQueryPort memberQueryPort;
    private final MemberCommandPort memberCommandPort;
    private final JwtProvider jwtProvider;

    public TokenResponse loginWithKakao(String code) {
        String accessToken = this.requestAccessTokenByCode(code);
        String email = this.requestUserEmailByAccessToken(accessToken);

        try {
            Long memberId = memberQueryPort.getIdByEmail(email);
            memberCommandPort.updateKakaoAccessToken(memberId, accessToken);
        } catch (NotFoundException e) {
            memberCommandPort.create(new MemberInfo(email, null, accessToken));
        }

        String token = jwtProvider.createToken(email);

        return new TokenResponse(token);
    }

    private String requestAccessTokenByCode(String code) {
        return kakaoLoginClient.requestAccessToken(code)
                .accessToken();
    }

    private String requestUserEmailByAccessToken(String accessToken) {
        return kakaoLoginClient.requestUserInfo(accessToken)
                .email();
    }
}
