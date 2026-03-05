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
        TransactionTemplate transactionTemplate
    ) {
        this.kakaoLoginClient = kakaoLoginClient;
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
        this.transactionTemplate = transactionTemplate;
    }

    public TokenResponse loginWithKakao(String code) {
        // external HTTP calls (outside transaction)
        KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
        KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(kakaoToken.accessToken());
        String email = kakaoUser.email();

        // DB operations (in transaction)
        Member member = transactionTemplate.execute(status -> {
            Member m = memberRepository.findByEmail(email)
                .orElseGet(() -> new Member(email));
            m.updateKakaoAccessToken(kakaoToken.accessToken());
            memberRepository.save(m);
            return m;
        });

        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }
}
