package gift.service;

import gift.auth.JwtProvider;
import gift.dto.TokenResponse;
import gift.model.Member;
import gift.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(MemberRepository memberRepository, MemberService memberService, JwtProvider jwtProvider, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.memberService = memberService;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TokenResponse register(String email, String password) {
        final Member member = memberService.create(email, password);
        final String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String email, String password) {
        final Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!member.passwordMatches(password, passwordEncoder)) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        final String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }
}
