package gift.member;

import jakarta.validation.constraints.NotBlank;

public record MemberRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}
