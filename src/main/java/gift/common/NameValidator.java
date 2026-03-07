package gift.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class NameValidator {
    private static final Pattern ALLOWED_PATTERN =
        Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ ()\\[\\]+\\-&/_]*$");

    private final String label;
    private final Rule[] rules;

    private NameValidator(String label, Rule[] rules) {
        this.label = label;
        this.rules = rules;
    }

    public static NameValidator of(String label, Rule... rules) {
        return new NameValidator(label, rules);
    }

    public void validateOrThrow(String name) {
        List<String> errors = validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    public List<String> validate(String name) {
        List<String> errors = new ArrayList<>();

        if (name == null || name.isBlank()) {
            errors.add(label + " is required.");
            return errors;
        }

        for (Rule rule : rules) {
            rule.check(name, label).ifPresent(errors::add);
        }

        return errors;
    }

    public static Rule maxLength(int max) {
        return (name, label) -> name.length() > max
            ? Optional.of(label + " must be at most " + max + " characters.")
            : Optional.empty();
    }

    public static Rule allowedCharacters() {
        return (name, label) -> !ALLOWED_PATTERN.matcher(name).matches()
            ? Optional.of(label + " contains invalid special characters. Allowed: ( ) [ ] + - & / _")
            : Optional.empty();
    }

    public static Rule noKakao() {
        return (name, label) -> name.contains("카카오")
            ? Optional.of(label + " containing \"카카오\" requires approval from the MD team.")
            : Optional.empty();
    }

    @FunctionalInterface
    public interface Rule {
        Optional<String> check(String name, String label);
    }
}