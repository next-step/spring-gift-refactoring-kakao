package gift.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Nested
    @DisplayName("chargePoint")
    class ChargePoint {

        @Test
        @DisplayName("정상 충전 시 포인트가 증가한다")
        void chargePoint() {
            Member member = new Member("test@test.com", "password");

            member.chargePoint(1_000);

            assertThat(member.getPoint()).isEqualTo(1_000);
        }

        @Test
        @DisplayName("0을 충전하면 예외가 발생한다")
        void chargeZero() {
            Member member = new Member("test@test.com", "password");

            assertThatThrownBy(() -> member.chargePoint(0))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("음수를 충전하면 예외가 발생한다")
        void chargeNegative() {
            Member member = new Member("test@test.com", "password");

            assertThatThrownBy(() -> member.chargePoint(-100))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("deductPoint")
    class DeductPoint {

        @Test
        @DisplayName("정상 차감 시 포인트가 감소한다")
        void deductPoint() {
            Member member = new Member("test@test.com", "password");
            member.chargePoint(10_000);

            member.deductPoint(3_000);

            assertThat(member.getPoint()).isEqualTo(7_000);
        }

        @Test
        @DisplayName("잔액과 동일한 금액을 차감하면 포인트가 0이 된다")
        void deductExactBalance() {
            Member member = new Member("test@test.com", "password");
            member.chargePoint(5_000);

            member.deductPoint(5_000);

            assertThat(member.getPoint()).isEqualTo(0);
        }

        @Test
        @DisplayName("잔액보다 큰 금액을 차감하면 예외가 발생한다")
        void deductMoreThanBalance() {
            Member member = new Member("test@test.com", "password");
            member.chargePoint(1_000);

            assertThatThrownBy(() -> member.deductPoint(5_000))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("0을 차감하면 예외가 발생한다")
        void deductZero() {
            Member member = new Member("test@test.com", "password");
            member.chargePoint(1_000);

            assertThatThrownBy(() -> member.deductPoint(0))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
