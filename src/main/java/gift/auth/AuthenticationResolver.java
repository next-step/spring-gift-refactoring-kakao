package gift.auth;

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
                    .orElseThrow(() -> new IllegalStateException("인증에 실패했습니다."));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("인증에 실패했습니다.");
        }
    }
}
