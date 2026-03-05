package gift.auth;

import gift.member.Member;
import gift.member.MemberService;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationResolver {
    private final JwtProvider jwtProvider;
    private final MemberService memberService;

    public AuthenticationResolver(JwtProvider jwtProvider, MemberService memberService) {
        this.jwtProvider = jwtProvider;
        this.memberService = memberService;
    }

    public Member extractMember(String authorization) {
        try {
            final String token = authorization.replace("Bearer ", "");
            final String email = jwtProvider.getEmail(token);
            return memberService.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("인증된 회원을 찾을 수 없습니다."));
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("유효하지 않은 인증 정보입니다.");
        }
    }
}
