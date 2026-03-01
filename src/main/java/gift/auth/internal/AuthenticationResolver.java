package gift.auth.internal;

import gift.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated member from an Authorization header.
 *
 * @author brian.kim
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class AuthenticationResolver {

    private final JwtProvider jwtProvider;
    private final AuthMemberRepository memberRepository;

    public Member extractMember(String authorization) {
        try {
            final String token = authorization.replace("Bearer ", "");
            final String email = jwtProvider.getEmail(token);
            return memberRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
