package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KakaoAuthService {
    private final KakaoLoginClient kakaoLoginClient;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public KakaoAuthService(
            KakaoLoginClient kakaoLoginClient, MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.kakaoLoginClient = kakaoLoginClient;
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    public TokenResponse handleCallback(String code) {
        final KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
        final KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(kakaoToken.accessToken());

        final Member member = saveOrUpdateMember(kakaoUser.email(), kakaoToken.accessToken());

        final String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    @Transactional
    protected Member saveOrUpdateMember(String email, String kakaoAccessToken) {
        final Member member = memberRepository.findByEmail(email).orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoAccessToken);
        return memberRepository.save(member);
    }
}
