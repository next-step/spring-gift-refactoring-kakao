package gift.product.common;

import java.util.regex.Pattern;

public class SpecialCharacterRule implements ProductNameRule {

    private static final Pattern ALLOWED_PATTERN =
            Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ ()\\[\\]+\\-&/_]*$");

    @Override
    public boolean notValid(String productName) {
        if (productName == null) {
            return false;
        }

        return !ALLOWED_PATTERN.matcher(productName).matches();
    }

    @Override
    public String describeRule() {
        return "상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _";
    }
}
