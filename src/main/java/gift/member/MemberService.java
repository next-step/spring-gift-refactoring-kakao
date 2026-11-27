package gift.member;

import gift.auth.JwtProvider;
import org.springframework.stereotype.Service;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    public String register(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        final Member member = memberRepository.save(new Member(email, password));
        return jwtProvider.createToken(member.getEmail());
    }

    public String login(String email, String password) {
        final Member member = memberRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (member.getPassword() == null || !member.getPassword().equals(password)) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return jwtProvider.createToken(member.getEmail());
    }

    public String loginWithKakao(String email, String kakaoAccessToken) {
        Member member = memberRepository.findByEmail(email).orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoAccessToken);
        memberRepository.save(member);

        return jwtProvider.createToken(member.getEmail());
    }
}
