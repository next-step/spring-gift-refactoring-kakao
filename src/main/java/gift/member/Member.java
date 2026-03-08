package gift.member;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Represents a registered member.
 *
 * @author brian.kim
 * @since 1.0
 */
@Entity
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String password;

    private String kakaoAccessToken;

    private int point;

    protected Member() {}

    public Member(String email, String rawPassword, PasswordEncoder passwordEncoder) {
        this.email = email;
        this.password = passwordEncoder.encode(rawPassword);
    }

    public Member(String email) {
        this.email = email;
    }

    public boolean checkPassword(String rawPassword, PasswordEncoder passwordEncoder) {
        if (this.password == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, this.password);
    }

    public void update(String email, String rawPassword, PasswordEncoder passwordEncoder) {
        this.email = email;
        this.password = passwordEncoder.encode(rawPassword);
    }

    public void updateKakaoAccessToken(String kakaoAccessToken) {
        this.kakaoAccessToken = kakaoAccessToken;
    }

    public void chargePoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 1 이상이어야 합니다.");
        }
        this.point += amount;
    }

    // point deduction for order payment
    public void deductPoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("차감 금액은 1 이상이어야 합니다.");
        }
        if (amount > this.point) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        this.point -= amount;
    }

    public Optional<String> getKakaoAccessTokenIfIntegrated() {
        return Optional.ofNullable(kakaoAccessToken);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public int getPoint() {
        return point;
    }
}
