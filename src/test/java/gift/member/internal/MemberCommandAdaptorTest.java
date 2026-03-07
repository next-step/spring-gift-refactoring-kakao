package gift.member.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gift.global.NotFoundException;
import gift.member.Member;
import gift.member.MemberCommandPort;
import gift.member.MemberInfo;
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
@Import(MemberCommandAdaptor.class)
class MemberCommandAdaptorTest {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    @Autowired
    MemberCommandPort memberCommandPort;

    @Autowired
    TestMemberRepository testMemberRepo;

    @AfterEach
    void tearDown() {
        testMemberRepo.deleteAllInBatch();
    }

    @Test
    @DisplayName("포인트를 차감한다")
    void testDeductPoint() {
        // given
        int point = 1_000;
        int deductAmount = 300;

        Long memberId = createMember(
                "a@b.com", "pw",
                null, point
        )
                .getId();

        // when + then
        assertThatCode(() -> memberCommandPort.deductPoint(memberId, deductAmount))
                .doesNotThrowAnyException();

        // then
        Member find = getMember(memberId);

        assertThat(find.getPoint())
                .isEqualTo(point - deductAmount);
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

    private Member getMember(Long id) {
        return testMemberRepo.findById(id)
                .orElseThrow(AssertionError::new);
    }

    @Test
    @DisplayName("포인트 잔액이 부족하면 IllegalArgumentException 이 발생한다")
    void testDeductPointInsufficientMemberPoint() {
        // given
        int point = 100;
        int deductAmount = point + 1;

        Long memberId = createMember(
                "a@b.com", "pw",
                null, point
        )
                .getId();

        // when + then
        assertThatThrownBy(() -> memberCommandPort.deductPoint(memberId, deductAmount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 포인트를 차감하면 NotFoundException 이 발생한다")
    void testDeductPointNotFound() {
        // when + then
        assertThatThrownBy(() -> memberCommandPort.deductPoint(NOT_EXISTING_ID, 100))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("회원을 생성한다")
    void testCreate() {
        // given
        MemberInfo info = new MemberInfo("a@b.com", "pw", "token");

        // when
        Long memberId = memberCommandPort.create(info);

        // then
        Member find = getMember(memberId);
        assertThat(find.getEmail()).isEqualTo(info.email());
        assertThat(find.getPassword()).isEqualTo(info.password());
        assertThat(find.getKakaoAccessToken()).isEqualTo(info.kakaoAccessToken());
    }

    @Test
    @DisplayName("카카오 액세스 토큰을 갱신한다")
    void testUpdateKakaoAccessToken() {
        // given
        String oldToken = "old-token";
        String newToken = "new-token";

        Long memberId = createMember(
                "a@b.com", "pw",
                oldToken, 0
        )
                .getId();

        // when
        memberCommandPort.updateKakaoAccessToken(memberId, newToken);

        // then
        Member find = getMember(memberId);
        assertThat(find.getKakaoAccessToken()).isEqualTo(newToken);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 카카오 액세스 토큰을 갱신하면 NotFoundException 이 발생한다")
    void testUpdateKakaoAccessTokenNotFound() {
        // when + then
        assertThatThrownBy(() -> memberCommandPort.updateKakaoAccessToken(NOT_EXISTING_ID, "tok"))
                .isInstanceOf(NotFoundException.class);
    }
}
