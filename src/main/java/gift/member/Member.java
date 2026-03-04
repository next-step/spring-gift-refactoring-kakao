package gift.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Getter
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true)
    private String email;

    private String password;

    @Column(length = 512)
    private String kakaoAccessToken;

    @NotNull
    private int point;

    protected Member() { }

    public Member(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public Member(String email) {
        this.email = email;
    }

    public void update(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public void updateKakaoAccessToken(String kakaoAccessToken) {
        this.kakaoAccessToken = kakaoAccessToken;
    }

    public void verifyPassword(String rawPassword, PasswordEncoder passwordEncoder) {
        if (this.password == null || !passwordEncoder.matches(rawPassword, this.password)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    public void chargePoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0 보다 커야 합니다.");
        }
        this.point += amount;
    }

    public void deductPoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("차감 금액은 1 이상이어야 합니다.");
        }
        if (amount > this.point) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        this.point -= amount;
    }
}
