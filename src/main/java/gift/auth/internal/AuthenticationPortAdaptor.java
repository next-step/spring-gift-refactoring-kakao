package gift.auth.internal;

import gift.auth.AuthenticationPort;
import gift.global.NotFoundException;
import gift.global.UnauthorizedException;
import gift.member.MemberQueryPort;
import io.jsonwebtoken.JwtException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationPortAdaptor implements AuthenticationPort {

    private final JwtProvider jwtProvider;
    private final MemberQueryPort memberQueryPort;

    @Override
    public Optional<Long> requestMemberIdFrom(String authorization) {
        Long memberId;

        try {
            memberId = this.getMemberIdFrom(authorization);
        } catch (UnauthorizedException ignored) {
            memberId = null;
        }

        return Optional.ofNullable(memberId);
    }

    @Override
    public Long getMemberIdFrom(String authorization) {
        String token = removeBearerPrefix(authorization);

        try {
            String memberEmail = jwtProvider.getEmail(token);

            return memberQueryPort.getIdByEmail(memberEmail);

        } catch (JwtException | NotFoundException e) {
            throw new UnauthorizedException("Invalid authorization");
        }
    }

    private static String removeBearerPrefix(String authorization) {
        return authorization.replace("Bearer ", "");
    }
}
