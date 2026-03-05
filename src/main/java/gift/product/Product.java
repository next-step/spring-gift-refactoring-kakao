package gift.product;

import gift.category.Category;
import gift.common.exception.ApplicationException;
import gift.option.Option;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "product")
public class Product {
    private static final int MAX_NAME_LENGTH = 15;
    private static final Pattern ALLOWED_NAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ ()\\[\\]+\\-&/_]*$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int price;
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Option> options = new ArrayList<>();

    public Product(String name, int price, String imageUrl, Category category) {
        validateName(name);
        validatePrice(price);
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    private void validatePrice(int price) {
        if (price <= 0) {
            throw new ApplicationException(ProductErrorCode.INVALID_PRICE);
        }
    }

    public void removeOption(Option option) {
        if (this.options.size() <= 1) {
            throw new ApplicationException(ProductErrorCode.LAST_OPTION_DELETE);
        }
        this.options.remove(option);
    }

    public void update(String name, int price, String imageUrl, Category category) {
        validateName(name);
        validatePrice(price);
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ApplicationException(ProductErrorCode.INVALID_NAME, "상품 이름은 필수입니다.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ApplicationException(ProductErrorCode.INVALID_NAME,
                    "상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다.");
        }
        if (!ALLOWED_NAME_PATTERN.matcher(name).matches()) {
            throw new ApplicationException(ProductErrorCode.INVALID_NAME,
                    "상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
        }
    }

}
