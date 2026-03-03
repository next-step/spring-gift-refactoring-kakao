package gift.product.admin;

import gift.product.common.MaxLengthRule;
import gift.product.common.NotBlankRule;
import gift.product.common.ProductNameRule;
import gift.product.common.ProductNameValidator;
import gift.product.common.SpecialCharacterRule;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdminProductNameValidator implements ProductNameValidator {

    private final List<ProductNameRule> nameRules;

    public AdminProductNameValidator() {
        this.nameRules = List.of(
                new NotBlankRule(),
                new MaxLengthRule(15),
                new SpecialCharacterRule()
        );
    }

    @Override
    public List<String> validate(String productName) {
        return nameRules.stream()
                .filter(r -> r.notValid(productName))
                .map(ProductNameRule::describeRule)
                .toList();
    }
}
