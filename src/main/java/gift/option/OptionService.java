package gift.option;

import gift.product.Product;
import gift.product.ProductRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    public OptionService(OptionRepository optionRepository, ProductRepository productRepository) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
    }

    public List<Option> getOptions(Long productId) {
        productRepository.findById(productId).orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));
        return optionRepository.findByProductId(productId);
    }

    @Transactional
    public Option createOption(Long productId, String name, int quantity) {
        validateName(name);
        final Product product =
                productRepository.findById(productId).orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));

        if (optionRepository.existsByProductIdAndName(productId, name)) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        return optionRepository.save(new Option(product, name, quantity));
    }

    @Transactional
    public void deleteOption(Long productId, Long optionId) {
        productRepository.findById(productId).orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));

        final List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        final Option option =
                optionRepository.findById(optionId).orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다."));

        if (!option.getProduct().getId().equals(productId)) {
            throw new NoSuchElementException("해당 상품의 옵션이 아닙니다.");
        }

        optionRepository.delete(option);
    }

    private void validateName(String name) {
        final List<String> errors = OptionNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
