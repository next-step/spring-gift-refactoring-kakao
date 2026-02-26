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
        return productRepository.findById(productId)
            .map(product -> optionRepository.findByProductId(productId).stream()
                .map(OptionResponse::from)
                .toList());
    }

    public Optional<OptionResponse> createOption(Long productId, OptionRequest request) {
        validateName(request.name());

        return productRepository.findById(productId)
            .map(product -> {
                if (optionRepository.existsByProductIdAndName(productId, request.name())) {
                    throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
                }
                Option saved = optionRepository.save(new Option(product, request.name(), request.quantity()));
                return OptionResponse.from(saved);
            });
    }

    public Optional<Boolean> deleteOption(Long productId, Long optionId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return Optional.empty();
        }

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        return optionRepository.findById(optionId)
            .filter(option -> option.getProduct().getId().equals(productId))
            .map(option -> {
                optionRepository.delete(option);
                return true;
            });
    }

    private void validateName(String name) {
        List<String> errors = OptionNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
