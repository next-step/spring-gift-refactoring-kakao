package gift.member.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gift.global.NotFoundException;
import gift.member.Member;
import gift.member.MemberQueryPort;
import gift.support.TestMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(MemberQueryAdaptor.class)
class MemberQueryAdaptorTest {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    @Autowired
    MemberQueryPort memberQueryPort;

    @Autowired
    TestMemberRepository testMemberRepo;

    @AfterEach
    void tearDown() {
        testMemberRepo.deleteAllInBatch();
    }

    @Test
    @DisplayName("존재하는 이메일로 회원 ID를 조회한다")
    void testGetIdByEmail() {
        // given
        String email = "a@b.com";

        Long memberId = createMember(email, "pw", null, 0)
                .getId();

        // when
        Long result = memberQueryPort.getIdByEmail(email);

        // then
        assertThat(result).isEqualTo(memberId);
    }

    @SuppressWarnings("SameParameterValue")
    private Member createMember(
            String email, String password,
            String kakaoAccessToken, int point
    ) {
        Member newEntity = Member.builder()
                .email(email)
                .password(password)
                .kakaoAccessToken(kakaoAccessToken)
                .point(point)
                .build();

        return testMemberRepo.save(newEntity);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 NotFoundException 이 발생한다")
    void testGetIdByEmailNotFound() {
        // when + then
        assertThatThrownBy(() -> memberQueryPort.getIdByEmail("x@y.com"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("존재하는 회원의 이메일을 조회한다")
    void testGetEmail() {
        // given
        String email = "a@b.com";

        Long memberId = createMember(email, "pw", null, 0)
                .getId();

        // when
        String result = memberQueryPort.getEmail(memberId);

        // then
        assertThat(result).isEqualTo(email);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 이메일을 조회하면 NotFoundException 이 발생한다")
    void testGetEmailNotFound() {
        // when + then
        assertThatThrownBy(() -> memberQueryPort.getEmail(NOT_EXISTING_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("존재하는 회원의 카카오 액세스 토큰을 조회한다")
    void testGetKakaoAccessToken() {
        // given
        String token = "i am token";

        Long memberId = createMember("a@b.com", "pw", token, 0)
                .getId();

        // when
        String result = memberQueryPort.getKakaoAccessToken(memberId);

        // then
        assertThat(result).isEqualTo(token);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 카카오 액세스 토큰을 조회하면 NotFoundException 이 발생한다")
    void testGetKakaoAccessTokenNotFound() {
        // when + then
        assertThatThrownBy(() -> memberQueryPort.getKakaoAccessToken(NOT_EXISTING_ID))
                .isInstanceOf(NotFoundException.class);
    }
}
