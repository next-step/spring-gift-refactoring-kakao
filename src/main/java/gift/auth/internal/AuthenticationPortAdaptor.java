package gift.auth.internal;

import gift.auth.AuthenticationPort;
import gift.global.NotFoundException;
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
    public Optional<Long> getMemberIdFrom(String authorization) {
        String token = removeBearerPrefix(authorization);

        Long memberId = null;

        try {
            String memberEmail = jwtProvider.getEmail(token);

            memberId = memberQueryPort.getIdByEmail(memberEmail);

        } catch (JwtException | NotFoundException ignored) {

        }

        return Optional.ofNullable(memberId);
    }

    private static String removeBearerPrefix(String authorization) {
        return authorization.replace("Bearer ", "");
    }
}
