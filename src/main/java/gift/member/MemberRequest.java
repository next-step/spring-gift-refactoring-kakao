package gift.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/* 회원 가입 및 로그인 요청 */
public record MemberRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {
    public Member toEntity() {
        return new Member(email, password);
    }
}
