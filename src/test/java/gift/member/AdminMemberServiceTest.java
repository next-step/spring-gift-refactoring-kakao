package gift.member;

import gift.member.entity.Member;
import gift.member.exception.MemberErrorCode;
import gift.member.exception.MemberException;
import gift.member.repository.MemberRepository;
import gift.member.service.AdminMemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AdminMemberService adminMemberService;

    @Test
    @DisplayName("회원 조회 성공 시 회원을 반환한다")
    void findByIdOrThrow_success_returnsMember() {
        Member member = new Member("test@example.com", "password");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        Member result = adminMemberService.findByIdOrThrow(1L);

        assertEquals(member, result);
    }

    @Test
    @DisplayName("회원이 없으면 MEMBER_NOT_FOUND 예외를 던진다")
    void findByIdOrThrow_notFound_throwsException() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        MemberException exception = assertThrows(MemberException.class, () -> adminMemberService.findByIdOrThrow(1L));

        assertEquals(MemberErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
    }
}
