package gift.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import gift.global.NotFoundException;
import gift.member.MemberQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtPortAdaptorTest {

    @InjectMocks
    JwtPortAdaptor jwtPortAdaptor;

    @Mock
    JwtProvider jwtProvider;

    @Mock
    MemberQueryPort memberQueryPort;

    @Test
    @DisplayName("JWT 발급 — getEmail 호출 + createToken 호출 → JWT 반환")
    void testIssueMemberJwt() {
        // given
        Long memberId = 1L;
        String email = "user@kakao.com";
        String jwt = "jwt-token";

        given(memberQueryPort.getEmail(memberId))
                .willReturn(email);
        given(jwtProvider.createToken(email))
                .willReturn(jwt);

        // when
        String response = jwtPortAdaptor.issueMemberJwt(memberId);

        // then
        assertThat(response).isEqualTo(jwt);
    }

    @Test
    @DisplayName("JWT 발급 시 회원이 없으면 NotFoundException 이 전파된다")
    void testIssueMemberJwtMemberNotFound() {
        // given
        Long memberId = Long.MAX_VALUE;

        given(memberQueryPort.getEmail(memberId))
                .willThrow(NotFoundException.memberNotFound());

        // when + then
        assertThatThrownBy(() -> jwtPortAdaptor.issueMemberJwt(memberId))
                .isInstanceOf(NotFoundException.class);
    }
}
