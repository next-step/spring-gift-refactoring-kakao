package gift.product;

import gift.category.Category;
import gift.option.Option;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Entity
@Getter
public class Product {
    private static final int NAME_MAX_LENGTH = 15;
    private static final Pattern NAME_ALLOWED_PATTERN =
        Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ ()\\[\\]+\\-&/_]*$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int price;

    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Option> options = new ArrayList<>();

    protected Product() { }

    public Product(String name, int price, String imageUrl, Category category) {
        validateName(name, false);
        validatePrice(price);
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public Product(String name, int price, String imageUrl, Category category, boolean allowKakao) {
        validateName(name, allowKakao);
        validatePrice(price);
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public void update(String name, int price, String imageUrl, Category category) {
        validateName(name, false);
        validatePrice(price);
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public void update(String name, int price, String imageUrl, Category category, boolean allowKakao) {
        validateName(name, allowKakao);
        validatePrice(price);
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public static List<String> validateNameErrors(String name, boolean allowKakao) {
        List<String> errors = new ArrayList<>();
        if (name == null || name.isBlank()) {
            errors.add("상품 이름은 필수입니다.");
            return errors;
        }
        if (name.length() > NAME_MAX_LENGTH) {
            errors.add("상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다.");
        }
        if (!NAME_ALLOWED_PATTERN.matcher(name).matches()) {
            errors.add("상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
        }
        if (!allowKakao && name.contains("카카오")) {
            errors.add("\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.");
        }
        return errors;
    }

    private void validateName(String name, boolean allowKakao) {
        List<String> errors = validateNameErrors(name, allowKakao);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    private void validatePrice(int price) {
        if (price <= 0) {
            throw new IllegalArgumentException("상품 가격은 0보다 커야 합니다.");
        }
    }
}
