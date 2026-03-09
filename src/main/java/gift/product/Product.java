package gift.product;

import gift.category.Category;
import gift.option.Option;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Entity
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

    protected Product() {
    }

    public Product(String name, int price, String imageUrl, Category category) {
        validateName(name, false);
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    private Product(String name, int price, String imageUrl, Category category, boolean allowKakao) {
        validateName(name, allowKakao);
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public static Product withKakaoAllowed(String name, int price, String imageUrl, Category category) {
        return new Product(name, price, imageUrl, category, true);
    }

    public void update(String name, int price, String imageUrl, Category category) {
        validateName(name, false);
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public void updateWithKakaoAllowed(String name, int price, String imageUrl, Category category) {
        validateName(name, true);
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    private static void validateName(String name, boolean allowKakao) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품 이름은 필수입니다.");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다.");
        }
        if (!NAME_ALLOWED_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                "상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
        }
        if (!allowKakao && name.contains("카카오")) {
            throw new IllegalArgumentException(
                "\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Category getCategory() {
        return category;
    }

    public List<Option> getOptions() {
        return options;
    }
}
