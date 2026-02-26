package gift.option;

import gift.product.Product;
import gift.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    public OptionService(OptionRepository optionRepository, ProductRepository productRepository) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
    }

    public List<Option> findByProductId(Long productId) {
        findProductById(productId);
        return optionRepository.findByProductId(productId);
    }

    public Option create(Long productId, OptionRequest request) {
        validateName(request.name());
        Product product = findProductById(productId);

        if (optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        return optionRepository.save(new Option(product, request.name(), request.quantity()));
    }

    public void delete(Long productId, Long optionId) {
        findProductById(productId);

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        Option option = optionRepository.findById(optionId)
            .filter(o -> o.getProduct().getId().equals(productId))
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. id=" + optionId));

        optionRepository.delete(option);
    }

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));
    }

    private void validateName(String name) {
        List<String> errors = OptionNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
