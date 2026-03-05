package gift.auth;

import gift.auth.oauth.OAuthClient;
import gift.auth.oauth.OAuthClientRegistry;
import gift.auth.oauth.OAuthUserInfo;
import gift.external.ExternalProvider;
import gift.member.Member;
import gift.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthService {
    private final OAuthClientRegistry oAuthClientRegistry;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public URI getLoginUri(ExternalProvider provider) {
        OAuthClient client = oAuthClientRegistry.get(provider);
        return client.getLoginUri();
    }

    @Transactional
    public TokenResponse handleCallback(ExternalProvider provider, String code) {
        OAuthClient client = oAuthClientRegistry.get(provider);
        OAuthUserInfo userInfo = client.getUserInfo(code);
        String email = userInfo.email();

        Member member = memberRepository.findByEmail(email)
            .orElseGet(() -> new Member(email));
        if (provider == ExternalProvider.KAKAO) {
            member.updateKakaoAccessToken(userInfo.accessToken());
        }
        memberRepository.save(member);

        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }
}
