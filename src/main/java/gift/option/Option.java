package gift.option;

import gift.product.Product;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

@Entity
@Table(name = "options")
@Getter
public class Option {
    private static final int NAME_MAX_LENGTH = 50;
    private static final Pattern NAME_ALLOWED_PATTERN =
        Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ ()\\[\\]+\\-&/_]*$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int quantity;

    protected Option() { }

    public Option(Product product, String name, int quantity) {
        validateName(name);
        this.product = product;
        this.name = name;
        this.quantity = quantity;
    }

    public void subtractQuantity(int amount) {
        if (amount > this.quantity) {
            throw new IllegalArgumentException("차감할 수량이 현재 재고보다 많습니다.");
        }
        this.quantity -= amount;
    }

    public void validateBelongsTo(Long productId) {
        if (!this.product.getId().equals(productId)) {
            throw new NoSuchElementException("해당 상품에 속한 옵션이 아닙니다.");
        }
    }

    private void validateName(String name) {
        List<String> errors = new ArrayList<>();
        if (name == null || name.isBlank()) {
            errors.add("옵션 이름은 필수입니다.");
            throw new IllegalArgumentException(String.join(", ", errors));
        }
        if (name.length() > NAME_MAX_LENGTH) {
            errors.add("옵션 이름은 공백을 포함하여 최대 50자까지 입력할 수 있습니다.");
        }
        if (!NAME_ALLOWED_PATTERN.matcher(name).matches()) {
            errors.add("옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
