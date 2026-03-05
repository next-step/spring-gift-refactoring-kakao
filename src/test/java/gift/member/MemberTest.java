package gift.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Nested
    @DisplayName("verifyPassword")
    class VerifyPassword {

        @Test
        @DisplayName("올바른 비밀번호면 예외가 발생하지 않는다")
        void success() {
            Member member = new Member("test@example.com", "password123");

            member.verifyPassword("password123");
        }

        @Test
        @DisplayName("비밀번호가 틀리면 예외가 발생한다")
        void wrongPassword() {
            Member member = new Member("test@example.com", "password123");

            assertThatThrownBy(() -> member.verifyPassword("wrong"))
                .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("비밀번호가 null이면 예외가 발생한다")
        void nullPassword() {
            Member member = new Member("test@example.com");

            assertThatThrownBy(() -> member.verifyPassword("any"))
                .isInstanceOf(MemberException.class);
        }
    }

    @Nested
    @DisplayName("chargePoint")
    class ChargePoint {

        @Test
        @DisplayName("양수 금액을 충전하면 포인트가 증가한다")
        void success() {
            Member member = new Member("test@example.com", "password123");

            member.chargePoint(1000);

            assertThat(member.getPoint()).isEqualTo(1000);
        }

        @Test
        @DisplayName("0 이하 금액을 충전하면 예외가 발생한다")
        void zeroAmount() {
            Member member = new Member("test@example.com", "password123");

            assertThatThrownBy(() -> member.chargePoint(0))
                .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("음수 금액을 충전하면 예외가 발생한다")
        void negativeAmount() {
            Member member = new Member("test@example.com", "password123");

            assertThatThrownBy(() -> member.chargePoint(-100))
                .isInstanceOf(MemberException.class);
        }
    }

    @Nested
    @DisplayName("deductPoint")
    class DeductPoint {

        @Test
        @DisplayName("보유 포인트 이내 금액을 차감하면 포인트가 감소한다")
        void success() {
            Member member = new Member("test@example.com", "password123");
            member.chargePoint(5000);

            member.deductPoint(3000);

            assertThat(member.getPoint()).isEqualTo(2000);
        }

        @Test
        @DisplayName("보유 포인트보다 큰 금액을 차감하면 예외가 발생한다")
        void insufficientPoint() {
            Member member = new Member("test@example.com", "password123");
            member.chargePoint(1000);

            assertThatThrownBy(() -> member.deductPoint(2000))
                .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("0 이하 금액을 차감하면 예외가 발생한다")
        void zeroAmount() {
            Member member = new Member("test@example.com", "password123");

            assertThatThrownBy(() -> member.deductPoint(0))
                .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("보유 포인트와 동일한 금액을 차감하면 포인트가 0이 된다")
        void exactAmount() {
            Member member = new Member("test@example.com", "password123");
            member.chargePoint(5000);

            member.deductPoint(5000);

            assertThat(member.getPoint()).isEqualTo(0);
        }
    }
}
