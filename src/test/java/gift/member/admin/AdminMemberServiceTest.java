package gift.member.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import gift.member.Member;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    @InjectMocks
    AdminMemberService adminMemberService;

    @Mock
    AdminMemberRepository memberRepo;

    @Test
    @DisplayName("전체 회원 목록을 MemberDto 리스트로 반환한다")
    void testGetAllMembers() {
        // given
        Member member1 = createMember(1L, "a@test.com", "pw1", 1000);
        Member member2 = createMember(2L, "b@test.com", "pw2", 2000);

        given(memberRepo.findAll())
                .willReturn(List.of(member1, member2));

        // when
        List<MemberDto> result = adminMemberService.getAllMembers();

        // then
        then(memberRepo).should()
                .findAll();

        assertThat(result).hasSize(2);

        List<MemberDto> expectedResponses = List.of(
                MemberDto.from(member1),
                MemberDto.from(member2)
        );

        assertThat(result).containsExactlyInAnyOrderElementsOf(expectedResponses);
    }

    private static Member createMember(Long id, String email, String password, int point) {
        Member member = Member.builder()
                .email(email)
                .password(password)
                .point(point)
                .build();

        setId(member, id);
        return member;
    }

    private static void setId(Member member, Long id) {
        try {
            Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    @DisplayName("회원이 없으면 빈 리스트를 반환한다")
    void testGetAllMembersEmpty() {
        // given
        given(memberRepo.findAll())
                .willReturn(Collections.emptyList());

        // when
        List<MemberDto> result = adminMemberService.getAllMembers();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("등록된 이메일이면 true를 반환한다")
    void testHasEmailRegistered() {
        // given
        given(memberRepo.existsByEmail("a@test.com"))
                .willReturn(true);

        // when
        boolean result = adminMemberService.hasEmailRegistered("a@test.com");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("미등록 이메일이면 false를 반환한다")
    void testHasEmailNotRegistered() {
        // given
        given(memberRepo.existsByEmail("unknown@test.com"))
                .willReturn(false);

        // when
        boolean result = adminMemberService.hasEmailRegistered("unknown@test.com");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("회원을 생성하고 저장한다")
    void testCreateMember() {
        // when
        adminMemberService.createMember("new@test.com", "pw");

        // then
        then(memberRepo).should().save(any(Member.class));
    }

    @Test
    @DisplayName("ID로 회원을 조회하고 MemberDto를 반환한다")
    void testFindMember() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, "a@test.com", "pw", 500);

        given(memberRepo.findById(memberId))
                .willReturn(Optional.of(member));

        // when
        MemberDto result = adminMemberService.findMember(memberId);

        // then
        then(memberRepo).should().findById(memberId);
        assertThat(result.id()).isEqualTo(memberId);
        assertThat(result.email()).isEqualTo(member.getEmail());
        assertThat(result.password()).isEqualTo(member.getPassword());
        assertThat(result.point()).isEqualTo(member.getPoint());
    }

    @Test
    @DisplayName("존재하지 않는 ID면 IllegalArgumentException을 던진다")
    void testFindMemberNotFound() {
        // given
        given(memberRepo.findById(NOT_EXISTING_ID))
                .willReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> adminMemberService.findMember(NOT_EXISTING_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("회원 이메일과 비밀번호를 수정한다")
    void testUpdateMember() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, "old@test.com", "old-pw", 0);

        given(memberRepo.findById(memberId))
                .willReturn(Optional.of(member));

        // when
        String newEmail = "new@test.com";
        String newPassword = "new-pw";
        assertThatCode(() -> adminMemberService.updateMember(
                memberId, newEmail, newPassword
        ))
                .doesNotThrowAnyException();

        // then
        assertThat(member.getEmail()).isEqualTo(newEmail);
        assertThat(member.getPassword()).isEqualTo(newPassword);
    }

    // -- fixtures --

    @Test
    @DisplayName("포인트를 충전한다")
    void testChargePoint() {
        // given
        Long memberId = 1L;
        int point = 1_000;
        Member member = createMember(memberId, "a@test.com", "pw", point);

        given(memberRepo.findById(memberId))
                .willReturn(Optional.of(member));

        // when
        int amount = 100;
        assertThatCode(() -> adminMemberService.chargePoint(memberId, amount))
                .doesNotThrowAnyException();

        // then
        assertThat(member.getPoint()).isEqualTo(point + amount);
    }

    @Test
    @DisplayName("회원을 삭제한다")
    void testDeleteMember() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, "a@test.com", "pw", 0);

        given(memberRepo.findById(memberId))
                .willReturn(Optional.of(member));

        // when
        assertThatCode(() -> adminMemberService.deleteMember(memberId))
                .doesNotThrowAnyException();

        // then
        then(memberRepo).should()
                .delete(member);
    }
}
