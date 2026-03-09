package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthLoginService {
    private final OAuthLoginClient oAuthLoginClient;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public OAuthLoginService(OAuthLoginClient oAuthLoginClient, MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.oAuthLoginClient = oAuthLoginClient;
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public TokenResponse login(String code) {
        String accessToken = oAuthLoginClient.requestAccessToken(code);
        String email = oAuthLoginClient.requestUserEmail(accessToken);
        Member member = findOrCreateByEmail(email);
        member.updateOAuthAccessToken(accessToken);
        return new TokenResponse(jwtProvider.createToken(member.getEmail()));
    }

    private Member findOrCreateByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(new Member(email)));
    }
}
