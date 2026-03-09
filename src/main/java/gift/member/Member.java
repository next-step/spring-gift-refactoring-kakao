package gift.member;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

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

    @Embedded
    private Password password;

    private String oauthAccessToken;

    private int point;

    protected Member() {
    }

    public Member(String email, String rawPassword) {
        this.email = email;
        this.password = Password.of(rawPassword);
    }

    public Member(String email) {
        this.email = email;
    }

    public void update(String email, String rawPassword) {
        this.email = email;
        this.password = Password.of(rawPassword);
    }

    public void authenticate(String rawPassword) {
        if (password == null || !password.matches(rawPassword)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    public void updateOAuthAccessToken(String oauthAccessToken) {
        this.oauthAccessToken = oauthAccessToken;
    }

    public void chargePoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 1 이상이어야 합니다.");
        }
        this.point += amount;
    }

    /** point deduction for order payment */
    public void deductPoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("차감 금액은 1 이상이어야 합니다.");
        }
        if (amount > this.point) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        this.point -= amount;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getOAuthAccessToken() {
        return oauthAccessToken;
    }

    public int getPoint() {
        return point;
    }
}
