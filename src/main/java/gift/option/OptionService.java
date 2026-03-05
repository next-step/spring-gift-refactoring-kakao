package gift.option;

import gift.product.Product;
import gift.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    public List<OptionResponse> getOptions(Long productId) {
        productRepository.findById(productId)
            .orElseThrow(() -> new OptionException(OptionErrorCode.PRODUCT_NOT_FOUND));
        return optionRepository.findByProductId(productId).stream()
            .map(OptionResponse::from)
            .toList();
    }

    public OptionResponse createOption(Long productId, OptionRequest request) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new OptionException(OptionErrorCode.PRODUCT_NOT_FOUND));
        if (optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new OptionException(OptionErrorCode.DUPLICATE_OPTION_NAME);
        }
        Option saved = optionRepository.save(request.toEntity(product));
        return OptionResponse.from(saved);
    }

    public void deleteOption(Long productId, Long optionId) {
        productRepository.findById(productId)
            .orElseThrow(() -> new OptionException(OptionErrorCode.PRODUCT_NOT_FOUND));
        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new OptionException(OptionErrorCode.CANNOT_DELETE_LAST_OPTION);
        }
        Option option = optionRepository.findById(optionId)
            .filter(o -> o.getProduct().getId().equals(productId))
            .orElseThrow(() -> new OptionException(OptionErrorCode.OPTION_NOT_FOUND));
        optionRepository.delete(option);
    }
}
