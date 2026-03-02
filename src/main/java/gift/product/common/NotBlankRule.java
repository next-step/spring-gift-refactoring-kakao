package gift.product.common;

public class NotBlankRule implements ProductNameRule {

    @Override
    public boolean notValid(String productName) {
        return productName == null || productName.isEmpty();
    }

    @Override
    public String describeRule() {
        return "상품 이름은 필수입니다.";
    }
}
