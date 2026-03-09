package gift.member;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Embeddable
public class Password {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Column(name = "password")
    private String value;

    protected Password() {
    }

    private Password(String value) {
        this.value = value;
    }

    public static Password of(String rawPassword) {
        return new Password(ENCODER.encode(rawPassword));
    }

    public static Password fromEncoded(String encoded) {
        return new Password(encoded);
    }

    public boolean matches(String rawPassword) {
        return ENCODER.matches(rawPassword, value);
    }
}
