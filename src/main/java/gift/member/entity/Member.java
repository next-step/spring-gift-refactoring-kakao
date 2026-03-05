package gift.member.entity;

import gift.member.exception.MemberErrorCode;
import gift.member.exception.MemberException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String password;

    private String kakaoAccessToken;

    private int point;

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
            throw new MemberException(MemberErrorCode.INVALID_CHARGE_AMOUNT);
        }
        this.point += amount;
    }

    public void deductPoint(int amount) {
        if (amount <= 0) {
            throw new MemberException(MemberErrorCode.INVALID_DEDUCT_AMOUNT);
        }
        if (amount > this.point) {
            throw new MemberException(MemberErrorCode.INSUFFICIENT_POINT);
        }
        this.point -= amount;
    }
}
