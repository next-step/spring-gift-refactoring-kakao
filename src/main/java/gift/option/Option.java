package gift.option;

import gift.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "option")
public class Option {
    private static final int MAX_NAME_LENGTH = 50;
    private static final Pattern ALLOWED_NAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ ()\\[\\]+\\-&/_]*$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String name;

    private int quantity;

    private static final int MAX_QUANTITY = 99_999_999;

    public Option(Product product, String name, int quantity) {
        validateName(name);
        validateQuantity(quantity);
        this.product = product;
        this.name = name;
        this.quantity = quantity;
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("옵션 수량은 1 이상 " + MAX_QUANTITY + " 이하여야 합니다.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("옵션 이름은 필수입니다.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("옵션 이름은 공백을 포함하여 최대 50자까지 입력할 수 있습니다.");
        }
        if (!ALLOWED_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
        }
    }

    public int calculateTotalPrice(int quantity) {
        return this.product.getPrice() * quantity;
    }

    public void subtractQuantity(int amount) {
        if (amount > this.quantity) {
            throw new IllegalArgumentException("차감할 수량이 현재 재고보다 많습니다.");
        }
        this.quantity -= amount;
    }

}
