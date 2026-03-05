package gift.auth.jwt;

import gift.member.entity.Member;
import gift.member.repository.MemberRepository;
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
    private final MemberRepository memberRepository;

    public Member extractMember(String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            String email = jwtProvider.getEmail(token);
            return memberRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
