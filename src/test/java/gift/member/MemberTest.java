package gift.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemberTest {
    @Test
    @DisplayName("포인트 충전 금액이 0 이하이면 INVALID_CHARGE_AMOUNT 예외를 던진다")
    void chargePoint_invalidAmount_throwsException() {
        Member member = new Member("test@example.com", "password");

        MemberException exception = assertThrows(MemberException.class, () -> member.chargePoint(0));

        assertEquals(MemberErrorCode.INVALID_CHARGE_AMOUNT, exception.getErrorCode());
    }

    @Test
    @DisplayName("포인트를 정상 충전하면 포인트가 증가한다")
    void chargePoint_success_increasesPoint() {
        Member member = new Member("test@example.com", "password");

        member.chargePoint(1000);

        assertEquals(1000, member.getPoint());
    }

    @Test
    @DisplayName("포인트 차감 금액이 0 이하이면 INVALID_DEDUCT_AMOUNT 예외를 던진다")
    void deductPoint_invalidAmount_throwsException() {
        Member member = new Member("test@example.com", "password");

        MemberException exception = assertThrows(MemberException.class, () -> member.deductPoint(0));

        assertEquals(MemberErrorCode.INVALID_DEDUCT_AMOUNT, exception.getErrorCode());
    }

    @Test
    @DisplayName("보유 포인트보다 큰 금액을 차감하면 INSUFFICIENT_POINT 예외를 던진다")
    void deductPoint_insufficientPoint_throwsException() {
        Member member = new Member("test@example.com", "password");
        member.chargePoint(500);

        MemberException exception = assertThrows(MemberException.class, () -> member.deductPoint(1000));

        assertEquals(MemberErrorCode.INSUFFICIENT_POINT, exception.getErrorCode());
    }

    @Test
    @DisplayName("포인트를 정상 차감하면 포인트가 감소한다")
    void deductPoint_success_decreasesPoint() {
        Member member = new Member("test@example.com", "password");
        member.chargePoint(1000);

        member.deductPoint(300);

        assertEquals(700, member.getPoint());
    }
}
