package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationResolver {
	private static final String BEARER_PREFIX = "Bearer ";
	private static final String AUTH_ERROR_MESSAGE = "유효하지 않은 인증 정보입니다.";
	private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

	public AuthenticationResolver(JwtProvider jwtProvider, MemberRepository memberRepository) {
        this.jwtProvider = jwtProvider;
        this.memberRepository = memberRepository;
    }

    public Member extractMember(String authorization) {

		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException(AUTH_ERROR_MESSAGE);
        }

        final String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedException(AUTH_ERROR_MESSAGE);
        }

        final String email;
        try {
            email = jwtProvider.getEmail(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException(AUTH_ERROR_MESSAGE);
        }

        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("인증된 사용자를 찾을 수 없습니다."));
    }
}
