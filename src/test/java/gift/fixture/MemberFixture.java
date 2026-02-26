package gift.fixture;

import gift.member.Member;

public class MemberFixture {

    public static Member 주문회원() {
        Member member = new Member("member@test.com", "password");
        member.chargePoint(10_000_000);
        return member;
    }
}
