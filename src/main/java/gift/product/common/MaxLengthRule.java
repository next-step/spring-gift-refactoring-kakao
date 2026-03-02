package gift.product.common;

public class MaxLengthRule implements ProductNameRule {

    private final int maxLength;

    public MaxLengthRule(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public boolean notValid(String productName) {
        return productName == null || maxLength < productName.length();
    }

    @Override
    public String describeRule() {
        return "상품 이름은 공백을 포함하여 최대 %d자까지 입력할 수 있습니다."
                .formatted(maxLength);
    }
}
