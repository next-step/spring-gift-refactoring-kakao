package gift.option;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gift.product.Product;
import gift.product.ProductRepository;

@Transactional
@Service
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    public OptionService(OptionRepository optionRepository, ProductRepository productRepository) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Option> findByProductId(Long productId) {
        validateProductExists(productId);
        return optionRepository.findByProductId(productId);
    }

    public Option create(Long productId, OptionRequest request) {
        validateName(request.name());
        Product product = findProduct(productId);

        if (optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        return optionRepository.save(new Option(product, request.name(), request.quantity()));
    }

    public Option update(Long productId, Long optionId, OptionRequest request) {
        validateName(request.name());
        validateProductExists(productId);

        Option option = optionRepository.findById(optionId)
            .filter(o -> o.getProduct().getId().equals(productId))
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다."));

        if (!option.getName().equals(request.name())
            && optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        option.update(request.name(), request.quantity());
        return option;
    }

    public void delete(Long productId, Long optionId) {
        validateProductExists(productId);

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        Option option = optionRepository.findById(optionId)
            .filter(o -> o.getProduct().getId().equals(productId))
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다."));

        optionRepository.delete(option);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));
    }

    private void validateProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new NoSuchElementException("상품이 존재하지 않습니다.");
        }
    }

    private void validateName(String name) {
        List<String> errors = OptionNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
