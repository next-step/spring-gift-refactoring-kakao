package gift.auth;

import gift.error.UnauthorizedException;
import gift.member.MemberRepository;
import org.springframework.stereotype.Component;

/* 인증 헤더에서 회원 ID를 추출하는 컴포넌트 */
@Component
public class AuthenticationResolver {
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    public AuthenticationResolver(JwtProvider jwtProvider, MemberRepository memberRepository) {
        this.jwtProvider = jwtProvider;
        this.memberRepository = memberRepository;
    }

    public Long extractMemberId(String authorization) {
        final String token = authorization.replace("Bearer ", "");
        final String email = jwtProvider.getEmail(token);
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("인증에 실패했습니다."))
            .getId();
    }
}
