package gift.member;

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

    private String password;

    private String kakaoAccessToken;

    private int point;

    protected Member() {
    }

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

    public void chargePoint(int amount) {
        if (amount <= 0) {
            throw new MemberException(MemberErrorCode.INVALID_POINT_AMOUNT);
        }
        this.point += amount;
    }

    // point deduction for order payment
    public void deductPoint(int amount) {
        if (amount <= 0) {
            throw new MemberException(MemberErrorCode.INVALID_POINT_AMOUNT);
        }
        if (amount > this.point) {
            throw new MemberException(MemberErrorCode.INSUFFICIENT_POINT);
        }
        this.point -= amount;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getKakaoAccessToken() {
        return kakaoAccessToken;
    }

    public int getPoint() {
        return point;
    }
}
