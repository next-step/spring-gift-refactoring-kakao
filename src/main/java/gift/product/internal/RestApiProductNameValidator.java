package gift.product.internal;

import gift.global.BadRequestException;
import gift.product.common.MaxLengthRule;
import gift.product.common.NotBlankRule;
import gift.product.common.NotContainsKakaoRule;
import gift.product.common.ProductNameRule;
import gift.product.common.ProductNameValidator;
import gift.product.common.SpecialCharacterRule;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RestApiProductNameValidator implements ProductNameValidator {

    private final List<ProductNameRule> nameRules;

    public RestApiProductNameValidator() {
        this.nameRules = List.of(
                new NotBlankRule(),
                new MaxLengthRule(15),
                new SpecialCharacterRule(),
                new NotContainsKakaoRule()
        );
    }

    public void validateOrThrowException(String productName) {
        List<String> violatedRules = this.validate(productName);

        if (!violatedRules.isEmpty()) {
            throw new BadRequestException(String.join(", ", violatedRules));
        }
    }

    @Override
    public List<String> validate(String productName) {
        return nameRules.stream()
                .filter(r -> r.notValid(productName))
                .map(ProductNameRule::describeRule)
                .toList();
    }
}
