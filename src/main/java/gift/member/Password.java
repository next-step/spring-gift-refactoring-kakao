package gift.member;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Embeddable
public class Password {
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Column(name = "password")
    private String value;

    protected Password() {
    }

    public Password(String rawPassword) {
        this.value = ENCODER.encode(rawPassword);
    }

    public boolean matches(String rawPassword) {
        return value != null && ENCODER.matches(rawPassword, value);
    }
}
