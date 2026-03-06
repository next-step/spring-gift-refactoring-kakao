package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class KakaoAuthService {
    private final KakaoLoginClient kakaoLoginClient;
    private final KakaoLoginProperties properties;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public KakaoAuthService(
            KakaoLoginClient kakaoLoginClient,
            KakaoLoginProperties properties,
            MemberRepository memberRepository,
            JwtProvider jwtProvider) {
        this.kakaoLoginClient = kakaoLoginClient;
        this.properties = properties;
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    public String buildAuthorizationUrl() {
        return new KakaoAuthorizationUrl(properties).toString();
    }

    @Transactional
    public TokenResponse handleCallback(String code) {
        final KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
        final KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(kakaoToken.accessToken());
        final String email = kakaoUser.email();

        final Member member = memberRepository.findByEmail(email).orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoToken.accessToken());
        memberRepository.save(member);

        final String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }
}
