package gift.auth.internal;

import gift.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final KakaoLoginClient kakaoLoginClient;
    private final AuthMemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public TokenResponse loginWithKakao(String code) {
        KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
        KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(
                kakaoToken.accessToken());
        String email = kakaoUser.email();

        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> Member.builder()
                        .email(email)
                        .build());

        member.updateKakaoAccessToken(kakaoToken.accessToken());
        memberRepository.save(member);

        String token = jwtProvider.createToken(member.getEmail());

        return new TokenResponse(token);
    }
}
