package gift.option;

import gift.product.Product;
import gift.product.ProductErrorCode;
import gift.product.ProductException;
import gift.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OptionCommandService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    public OptionCommandService(OptionRepository optionRepository, ProductRepository productRepository) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
    }

    public Option createOption(Long productId, String name, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (optionRepository.existsByProductIdAndName(productId, name)) {
            throw new OptionException(OptionErrorCode.DUPLICATE_OPTION_NAME);
        }

        return optionRepository.save(new Option(product, name, quantity));
    }

    public void deleteOption(Long productId, Long optionId) {
        productRepository.findById(productId)
            .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new OptionException(OptionErrorCode.CANNOT_DELETE_LAST_OPTION);
        }

        Option option = optionRepository.findById(optionId).orElse(null);
        if (option == null || !option.getProduct().getId().equals(productId)) {
            throw new OptionException(OptionErrorCode.OPTION_NOT_FOUND);
        }

        optionRepository.delete(option);
    }
}
