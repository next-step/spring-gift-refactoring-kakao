package gift.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Nested
    @DisplayName("checkPassword")
    class CheckPassword {

        @Test
        @DisplayName("올바른 비밀번호면 true를 반환한다")
        void correctPassword() {
            Member member = new Member("test@email.com", "password123");
            assertThat(member.checkPassword("password123")).isTrue();
        }

        @Test
        @DisplayName("잘못된 비밀번호면 false를 반환한다")
        void wrongPassword() {
            Member member = new Member("test@email.com", "password123");
            assertThat(member.checkPassword("wrong")).isFalse();
        }

        @Test
        @DisplayName("비밀번호가 null인 회원은 false를 반환한다")
        void nullPassword() {
            Member member = new Member("test@email.com");
            assertThat(member.checkPassword("anything")).isFalse();
        }
    }

    @Nested
    @DisplayName("chargePoint")
    class ChargePoint {

        @Test
        @DisplayName("양수 금액을 충전하면 포인트가 증가한다")
        void chargePositiveAmount() {
            Member member = new Member("test@email.com", "pw");
            member.chargePoint(1000);
            assertThat(member.getPoint()).isEqualTo(1000);
        }

        @Test
        @DisplayName("0 이하 금액은 충전할 수 없다")
        void chargeZeroOrNegative() {
            Member member = new Member("test@email.com", "pw");
            assertThatThrownBy(() -> member.chargePoint(0))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> member.chargePoint(-100))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("deductPoint")
    class DeductPoint {

        @Test
        @DisplayName("보유 포인트 이내에서 차감할 수 있다")
        void deductWithinBalance() {
            Member member = new Member("test@email.com", "pw");
            member.chargePoint(5000);
            member.deductPoint(3000);
            assertThat(member.getPoint()).isEqualTo(2000);
        }

        @Test
        @DisplayName("보유 포인트를 초과하면 차감할 수 없다")
        void deductExceedingBalance() {
            Member member = new Member("test@email.com", "pw");
            member.chargePoint(1000);
            assertThatThrownBy(() -> member.deductPoint(1001))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("0 이하 금액은 차감할 수 없다")
        void deductZeroOrNegative() {
            Member member = new Member("test@email.com", "pw");
            member.chargePoint(1000);
            assertThatThrownBy(() -> member.deductPoint(0))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
