package gift.product.common;

public class NotContainsKakaoRule implements ProductNameRule {

    private static final String KAKAO = "카카오";

    @Override
    public boolean notValid(String productName) {
        return productName == null || productName.contains(KAKAO);
    }

    @Override
    public String describeRule() {
        return "\"%s\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다."
                .formatted(KAKAO);
    }
}
