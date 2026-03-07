package gift.member;

import jakarta.persistence.Embedded;
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

  @Embedded private Password password;

  private String kakaoAccessToken;

  private int point;

  public Member(String email, String password) {
    this.email = email;
    this.password = Password.of(password);
  }

  public Member(String email) {
    this.email = email;
  }

  public void validatePassword(String rawPassword) {
    if (this.password == null) {
      throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
    this.password.validate(rawPassword);
  }

  public void update(String email, String password) {
    this.email = email;
    this.password = Password.of(password);
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
