package gift.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gift.member.Member;
import gift.member.MemberRepository;

@Service
public class KakaoAuthService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public KakaoAuthService(
        MemberRepository memberRepository,
        JwtProvider jwtProvider
    ) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public String loginOrRegister(String email, String kakaoAccessToken) {
        Member member = memberRepository.findByEmail(email)
            .orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoAccessToken);
        memberRepository.save(member);
        return jwtProvider.createToken(member.getEmail());
    }
}
