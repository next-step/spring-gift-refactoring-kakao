package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KakaoMemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public KakaoMemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public TokenResponse registerOrUpdate(String email, String kakaoAccessToken) {
        Member member = memberRepository.findByEmail(email)
            .orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoAccessToken);
        memberRepository.save(member);

        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }
}
