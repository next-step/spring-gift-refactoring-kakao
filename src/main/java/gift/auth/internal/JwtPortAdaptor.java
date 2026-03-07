package gift.auth.internal;

import gift.auth.JwtPort;
import gift.member.MemberQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtPortAdaptor implements JwtPort {

    private final JwtProvider jwtProvider;
    private final MemberQueryPort memberQueryPort;

    @Override
    public String issueMemberJwt(Long memberId) {
        String memberEmail = memberQueryPort.getEmail(memberId);

        return jwtProvider.createToken(memberEmail);
    }
}
