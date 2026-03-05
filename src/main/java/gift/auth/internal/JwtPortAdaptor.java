package gift.auth.internal;

import gift.auth.JwtPort;
import gift.global.NotFoundException;
import gift.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtPortAdaptor implements JwtPort {

    private final JwtProvider jwtProvider;

    private final AuthMemberRepository memberRepo;

    @Override
    public String issueMemberJwt(Long memberId) {

        Member find = memberRepo.findById(memberId)
                .orElseThrow(NotFoundException::memberNotFound);

        String memberEmail = find.getEmail();

        return jwtProvider.createToken(memberEmail);
    }
}
