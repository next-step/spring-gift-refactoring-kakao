package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationResolver {
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    public AuthenticationResolver(JwtProvider jwtProvider, MemberRepository memberRepository) {
        this.jwtProvider = jwtProvider;
        this.memberRepository = memberRepository;
    }

    public Member extractMember(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("인증 정보가 유효하지 않습니다.");
        }
        String email = jwtProvider.getEmail(authorization.substring(7))
            .orElseThrow(() -> new UnauthorizedException("인증 정보가 유효하지 않습니다."));
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("인증 정보가 유효하지 않습니다."));
    }
}
