package gift.auth;

import gift.common.exception.ApplicationException;
import gift.member.Member;
import gift.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AuthenticationResolver {
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    public Member extractMember(String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            String email = jwtProvider.getEmail(token);
            return memberRepository.findByEmail(email)
                    .orElseThrow(() -> new ApplicationException(AuthErrorCode.AUTHENTICATION_FAILED));
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(AuthErrorCode.AUTHENTICATION_FAILED);
        }
    }
}
