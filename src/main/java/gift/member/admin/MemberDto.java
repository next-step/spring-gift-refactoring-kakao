package gift.member.admin;

import gift.member.Member;

public record MemberDto(
        Long id,
        String email,
        String password,
        int point
) {

    public static MemberDto from(Member entity) {
        Long id = entity.getId();
        String email = entity.getEmail();
        String password = entity.getPassword();
        int point = entity.getPoint();

        return new MemberDto(
                id, email, password, point
        );
    }
}
