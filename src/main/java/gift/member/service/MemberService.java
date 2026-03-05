package gift.member.service;

import gift.auth.jwt.JwtProvider;
import gift.auth.dto.TokenResponse;
import gift.member.dto.MemberRequest;
import gift.member.entity.Member;
import gift.member.exception.MemberErrorCode;
import gift.member.exception.MemberException;
import gift.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    @Transactional
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

    @Transactional
    public Member save(Member member) {
        return memberRepository.save(member);
    }
}
