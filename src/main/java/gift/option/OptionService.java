package gift.option;

import gift.product.Product;
import gift.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    public OptionService(OptionRepository optionRepository, ProductRepository productRepository) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
    }

    public Optional<List<OptionResponse>> getOptions(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return Optional.empty();
        }
        List<OptionResponse> options = optionRepository.findByProductId(productId).stream()
            .map(OptionResponse::from)
            .toList();
        return Optional.of(options);
    }

    public Optional<OptionResponse> createOption(Long productId, OptionRequest request) {
        validateName(request.name());

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return Optional.empty();
        }

        if (optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        Option saved = optionRepository.save(new Option(product, request.name(), request.quantity()));
        return Optional.of(OptionResponse.from(saved));
    }

    public Optional<Boolean> deleteOption(Long productId, Long optionId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return Optional.empty();
        }

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        Option option = optionRepository.findById(optionId).orElse(null);
        if (option == null || !option.getProduct().getId().equals(productId)) {
            return Optional.empty();
        }

        optionRepository.delete(option);
        return Optional.of(true);
    }

    private void validateName(String name) {
        List<String> errors = OptionNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
