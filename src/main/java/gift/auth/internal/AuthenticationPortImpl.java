package gift.auth.internal;

import gift.auth.AuthenticationPort;
import gift.member.Member;
import io.jsonwebtoken.JwtException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationPortImpl implements AuthenticationPort {

    private final JwtProvider jwtProvider;

    private final AuthMemberRepository memberRepo;

    @Override
    public Optional<Long> getMemberIdFrom(String authorization) {
        String token = removeBearerPrefix(authorization);

        Long memberId = null;

        try {
            String memberEmail = jwtProvider.getEmail(token);

            memberId = memberRepo.findByEmail(memberEmail).map(Member::getId).orElse(null);

        } catch (JwtException ignored) {

        }

        return Optional.ofNullable(memberId);
    }

    private static String removeBearerPrefix(String authorization) {
        return authorization.replace("Bearer ", "");
    }
}
