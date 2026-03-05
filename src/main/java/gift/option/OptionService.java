package gift.option;

import gift.product.Product;
import gift.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductService productService;

    public List<Option> findByProductId(Long productId) {
        productService.findById(productId);
        return optionRepository.findByProductId(productId);
    }

    public Option findById(Long id) {
        return optionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("옵션을 찾을 수 없습니다. id: " + id));
    }

    @Transactional
    public Option subtractQuantity(Long id, int amount) {
        Option option = findById(id);
        option.subtractQuantity(amount);
        return optionRepository.save(option);
    }

    @Transactional
    public Option create(Long productId, String name, int quantity) {
        validateName(name);
        Product product = productService.findById(productId);

        if (optionRepository.existsByProductIdAndName(productId, name)) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        return optionRepository.save(new Option(product, name, quantity));
    }

    @Transactional
    public void delete(Long productId, Long optionId) {
        Product product = productService.findById(productId);

        Option option = product.getOptions().stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("옵션을 찾을 수 없습니다. id: " + optionId));

        product.removeOption(option);
    }

    private void validateName(String name) {
        List<String> errors = OptionNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
