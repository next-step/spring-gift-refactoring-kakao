package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class KakaoAuthService {
    private final KakaoLoginClient kakaoLoginClient;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    private final TransactionTemplate transactionTemplate;

    public KakaoAuthService(
            KakaoLoginClient kakaoLoginClient,
            MemberRepository memberRepository,
            JwtProvider jwtProvider,
            TransactionTemplate transactionTemplate) {
        this.kakaoLoginClient = kakaoLoginClient;
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
        this.transactionTemplate = transactionTemplate;
    }

    public TokenResponse handleCallback(String code) {
        final KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
        final KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(kakaoToken.accessToken());

        final Member member = transactionTemplate.execute(status -> {
            final Member m =
                    memberRepository.findByEmail(kakaoUser.email()).orElseGet(() -> new Member(kakaoUser.email()));
            m.updateKakaoAccessToken(kakaoToken.accessToken());
            return memberRepository.save(m);
        });

        final String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }
}
