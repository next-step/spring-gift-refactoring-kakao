package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationResolver {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationResolver.class);
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    public AuthenticationResolver(JwtProvider jwtProvider, MemberRepository memberRepository) {
        this.jwtProvider = jwtProvider;
        this.memberRepository = memberRepository;
    }

    public Member extractMember(String authorization) {
        try {
            final String token = authorization.replace("Bearer ", "");
            final String email = jwtProvider.getEmail(token);
            return memberRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            log.warn("인증 토큰 처리 실패: {}", e.getMessage());
            return null;
        }
    }
}
