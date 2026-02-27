package gift.option;

import gift.product.Product;
import gift.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    public OptionService(OptionRepository optionRepository, ProductRepository productRepository) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
    }

    public List<Option> findByProductId(Long productId) {
        productRepository.findById(productId).orElseThrow();
        return optionRepository.findByProductId(productId);
    }

    @Transactional
    public Option create(Long productId, OptionRequest request) {
        validateName(request.name());
        Product product = productRepository.findById(productId).orElseThrow();
        if (optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }
        return optionRepository.save(new Option(product, request.name(), request.quantity()));
    }

    @Transactional
    public void delete(Long productId, Long optionId) {
        productRepository.findById(productId).orElseThrow();
        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }
        Option option = optionRepository.findById(optionId)
            .filter(o -> o.getProduct().getId().equals(productId))
            .orElseThrow();
        optionRepository.delete(option);
    }

    private void validateName(String name) {
        List<String> errors = OptionNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
