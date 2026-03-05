package gift.member;

public record MemberResponse(
    Long id,
    String email,
    int point
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
            member.getId(),
            member.getEmail(),
            member.getPoint()
        );
    }
}
