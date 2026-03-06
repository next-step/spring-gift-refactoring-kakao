package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the authenticated member from an Authorization header.
 *
 * @author brian.kim
 * @since 1.0
 */
@Component
public class AuthenticationResolver {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationResolver.class);

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    public AuthenticationResolver(JwtProvider jwtProvider, MemberRepository memberRepository) {
        this.jwtProvider = jwtProvider;
        this.memberRepository = memberRepository;
    }

    public Optional<Member> extractMember(String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            String email = jwtProvider.getEmail(token);
            return memberRepository.findByEmail(email);
        } catch (Exception e) {
            log.debug("인증 토큰 파싱 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
