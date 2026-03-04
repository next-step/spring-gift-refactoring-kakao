package gift.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginMemberRequest(@NotBlank @Email String email, @NotBlank String password) {}
