package gift.member;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;
import org.springframework.stereotype.Service;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    public TokenResponse register(MemberRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new MemberException(MemberErrorCode.DUPLICATE_EMAIL);
        }
        Member member = memberRepository.save(request.toEntity());
        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    public TokenResponse login(MemberRequest request) {
        Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new MemberException(MemberErrorCode.INVALID_CREDENTIALS));
        if (member.getPassword() == null || !member.getPassword().equals(request.password())) {
            throw new MemberException(MemberErrorCode.INVALID_CREDENTIALS);
        }
        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }
}
