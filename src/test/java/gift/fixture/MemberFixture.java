package gift.fixture;

import gift.member.Member;

public class MemberFixture {

    public static Member 주문회원() {
        return 회원(10_000_000);
    }

    public static Member 회원(int point) {
        Member member = new Member("member@test.com", "password");
        member.chargePoint(point);
        return member;
    }
}
