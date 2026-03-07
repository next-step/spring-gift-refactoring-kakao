package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationResolver {
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtProvider jwtProvider;
  private final MemberRepository memberRepository;

  public AuthenticationResolver(JwtProvider jwtProvider, MemberRepository memberRepository) {
    this.jwtProvider = jwtProvider;
    this.memberRepository = memberRepository;
  }

  public Member extractMember(String authorization) {
    String token = authorization.replace(BEARER_PREFIX, "");
    String email = jwtProvider.getEmail(token);
    return memberRepository
        .findByEmail(email)
        .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));
  }
}
