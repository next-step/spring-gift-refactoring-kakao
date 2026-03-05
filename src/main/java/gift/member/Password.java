package gift.member;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Password {
  private static final int SALT_LENGTH = 16;

  @Column(name = "password_hash")
  private String hash;

  @Column(name = "password_salt")
  private String salt;

  private Password(String hash, String salt) {
    this.hash = hash;
    this.salt = salt;
  }

  public static Password of(String rawPassword) {
    String salt = generateSalt();
    String hash = hashWith(rawPassword, salt);
    return new Password(hash, salt);
  }

  public void validate(String rawPassword) {
    if (hash == null || !hash.equals(hashWith(rawPassword, salt))) {
      throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
  }

  private static String generateSalt() {
    byte[] saltBytes = new byte[SALT_LENGTH];
    new SecureRandom().nextBytes(saltBytes);
    return Base64.getEncoder().encodeToString(saltBytes);
  }

  private static String hashWith(String rawPassword, String salt) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(salt.getBytes(StandardCharsets.UTF_8));
      byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
