package gift.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Nested
    @DisplayName("chargePoint")
    class ChargePoint {

        @Test
        @DisplayName("양수 금액을 충전하면 포인트가 증가한다")
        void chargePositiveAmount() {
            Member member = new Member("test@test.com", "password");

            member.chargePoint(1000);

            assertEquals(1000, member.getPoint());
        }

        @Test
        @DisplayName("여러 번 충전하면 포인트가 누적된다")
        void chargeMultipleTimes() {
            Member member = new Member("test@test.com", "password");

            member.chargePoint(1000);
            member.chargePoint(500);

            assertEquals(1500, member.getPoint());
        }

        @Test
        @DisplayName("0 이하 금액을 충전하면 예외가 발생한다")
        void chargeZeroOrNegativeThrows() {
            Member member = new Member("test@test.com", "password");

            assertThrows(IllegalArgumentException.class, () -> member.chargePoint(0));
            assertThrows(IllegalArgumentException.class, () -> member.chargePoint(-1));
        }
    }

    @Nested
    @DisplayName("deductPoint")
    class DeductPoint {

        @Test
        @DisplayName("잔액 이내 금액을 차감하면 포인트가 감소한다")
        void deductWithinBalance() {
            Member member = new Member("test@test.com", "password");
            member.chargePoint(1000);

            member.deductPoint(300);

            assertEquals(700, member.getPoint());
        }

        @Test
        @DisplayName("잔액과 동일한 금액을 차감하면 포인트가 0이 된다")
        void deductExactBalance() {
            Member member = new Member("test@test.com", "password");
            member.chargePoint(1000);

            member.deductPoint(1000);

            assertEquals(0, member.getPoint());
        }

        @Test
        @DisplayName("잔액을 초과하는 금액을 차감하면 예외가 발생한다")
        void deductOverBalanceThrows() {
            Member member = new Member("test@test.com", "password");
            member.chargePoint(1000);

            assertThrows(IllegalArgumentException.class, () -> member.deductPoint(1001));
        }

        @Test
        @DisplayName("0 이하 금액을 차감하면 예외가 발생한다")
        void deductZeroOrNegativeThrows() {
            Member member = new Member("test@test.com", "password");
            member.chargePoint(1000);

            assertThrows(IllegalArgumentException.class, () -> member.deductPoint(0));
            assertThrows(IllegalArgumentException.class, () -> member.deductPoint(-1));
        }
    }
}
