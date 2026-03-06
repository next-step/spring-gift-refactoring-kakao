package gift.model;

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
        @DisplayName("양수 금액을 충전하면 포인트가 증가한다")
        void chargePositiveAmount() {
            var member = new Member("test@example.com", "password");

            member.chargePoint(1000);

            assertThat(member.getPoint()).isEqualTo(1000);
        }

        @Test
        @DisplayName("여러 번 충전하면 포인트가 누적된다")
        void chargeMultipleTimes() {
            var member = new Member("test@example.com", "password");

            member.chargePoint(1000);
            member.chargePoint(2000);

            assertThat(member.getPoint()).isEqualTo(3000);
        }

        @Test
        @DisplayName("0원을 충전하면 예외가 발생한다")
        void chargeZeroThrows() {
            var member = new Member("test@example.com", "password");

            assertThatThrownBy(() -> member.chargePoint(0))
                .isInstanceOf(IllegalArgumentException.class);
            assertThat(member.getPoint()).isEqualTo(0);
        }

        @Test
        @DisplayName("음수 금액을 충전하면 예외가 발생한다")
        void chargeNegativeThrows() {
            var member = new Member("test@example.com", "password");

            assertThatThrownBy(() -> member.chargePoint(-100))
                .isInstanceOf(IllegalArgumentException.class);
            assertThat(member.getPoint()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("deductPoint")
    class DeductPoint {

        @Test
        @DisplayName("잔액 내의 금액을 차감하면 포인트가 줄어든다")
        void deductValidAmount() {
            var member = new Member("test@example.com", "password");
            member.chargePoint(5000);

            member.deductPoint(3000);

            assertThat(member.getPoint()).isEqualTo(2000);
        }

        @Test
        @DisplayName("잔액과 동일한 금액을 차감하면 포인트가 0이 된다")
        void deductExactBalance() {
            var member = new Member("test@example.com", "password");
            member.chargePoint(1000);

            member.deductPoint(1000);

            assertThat(member.getPoint()).isEqualTo(0);
        }

        @Test
        @DisplayName("잔액을 초과하는 금액을 차감하면 예외가 발생한다")
        void deductExceedingBalanceThrows() {
            var member = new Member("test@example.com", "password");
            member.chargePoint(1000);

            assertThatThrownBy(() -> member.deductPoint(1001))
                .isInstanceOf(IllegalArgumentException.class);
            assertThat(member.getPoint()).isEqualTo(1000);
        }

        @Test
        @DisplayName("0원을 차감하면 예외가 발생한다")
        void deductZeroThrows() {
            var member = new Member("test@example.com", "password");
            member.chargePoint(1000);

            assertThatThrownBy(() -> member.deductPoint(0))
                .isInstanceOf(IllegalArgumentException.class);
            assertThat(member.getPoint()).isEqualTo(1000);
        }

        @Test
        @DisplayName("음수 금액을 차감하면 예외가 발생한다")
        void deductNegativeThrows() {
            var member = new Member("test@example.com", "password");
            member.chargePoint(1000);

            assertThatThrownBy(() -> member.deductPoint(-100))
                .isInstanceOf(IllegalArgumentException.class);
            assertThat(member.getPoint()).isEqualTo(1000);
        }

        @Test
        @DisplayName("포인트가 0인 상태에서 차감하면 예외가 발생한다")
        void deductFromZeroBalanceThrows() {
            var member = new Member("test@example.com", "password");

            assertThatThrownBy(() -> member.deductPoint(1))
                .isInstanceOf(IllegalArgumentException.class);
            assertThat(member.getPoint()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("이메일과 비밀번호를 변경한다")
        void updateEmailAndPassword() {
            var member = new Member("old@example.com", "oldPassword");

            member.update("new@example.com", "newPassword");

            assertThat(member.getEmail()).isEqualTo("new@example.com");
            assertThat(member.getPassword()).isEqualTo("newPassword");
        }
    }

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("이메일과 비밀번호로 생성하면 초기 포인트는 0이다")
        void initialPointIsZero() {
            var member = new Member("test@example.com", "password");

            assertThat(member.getPoint()).isEqualTo(0);
        }

        @Test
        @DisplayName("이메일만으로 생성할 수 있다 (카카오 회원)")
        void createWithEmailOnly() {
            var member = new Member("kakao@example.com");

            assertThat(member.getEmail()).isEqualTo("kakao@example.com");
            assertThat(member.getPassword()).isNull();
        }
    }

    @Nested
    @DisplayName("updateKakaoAccessToken")
    class UpdateKakaoAccessToken {

        @Test
        @DisplayName("카카오 액세스 토큰을 업데이트한다")
        void updateToken() {
            var member = new Member("test@example.com", "password");

            member.updateKakaoAccessToken("new-kakao-token");

            assertThat(member.getKakaoAccessToken()).isEqualTo("new-kakao-token");
        }
    }
}
