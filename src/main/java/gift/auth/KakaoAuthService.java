package gift.auth;

import gift.member.Member;
import gift.member.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class KakaoAuthService {
    private final KakaoLoginClient kakaoLoginClient;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    public KakaoAuthService(KakaoLoginClient kakaoLoginClient, MemberService memberService, JwtProvider jwtProvider) {
        this.kakaoLoginClient = kakaoLoginClient;
        this.memberService = memberService;
        this.jwtProvider = jwtProvider;
    }

    public TokenResponse loginOrRegister(String code) {
        KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
        KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(kakaoToken.accessToken());

        Member member = memberService.findOrCreateByEmailAndUpdateKakaoToken(
            kakaoUser.email(), kakaoToken.accessToken());

        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }
}
