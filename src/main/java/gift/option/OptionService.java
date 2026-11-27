package gift.option;

import gift.product.Product;
import gift.product.ProductRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/*
 * Each product must have at least one option at all times.
 * Option names are validated against allowed characters and length constraints.
 */
@Service
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    public OptionService(OptionRepository optionRepository, ProductRepository productRepository) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
    }

    public enum DeleteResult {
        PRODUCT_NOT_FOUND,
        OPTION_NOT_FOUND,
        DELETED
    }

    public Optional<List<OptionResponse>> findOptions(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return Optional.empty();
        }
        List<OptionResponse> options = optionRepository.findByProductId(productId).stream()
                .map(OptionResponse::from)
                .collect(Collectors.toList());
        return Optional.of(options);
    }

    public Optional<Option> createOption(Long productId, OptionRequest request) {
        validateName(request.name());

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return Optional.empty();
        }

        if (optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        Option saved = optionRepository.save(new Option(product, request.name(), request.quantity()));
        return Optional.of(saved);
    }

    public DeleteResult deleteOption(Long productId, Long optionId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return DeleteResult.PRODUCT_NOT_FOUND;
        }

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        Option option = optionRepository.findById(optionId).orElse(null);
        if (option == null || !option.getProduct().getId().equals(productId)) {
            return DeleteResult.OPTION_NOT_FOUND;
        }

        optionRepository.delete(option);
        return DeleteResult.DELETED;
    }

    private void validateName(String name) {
        List<String> errors = OptionNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
