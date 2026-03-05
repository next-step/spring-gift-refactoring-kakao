package gift.option;

import gift.common.exception.ApplicationException;
import gift.product.Product;
import gift.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                .orElseThrow(() -> new ApplicationException(OptionErrorCode.NOT_FOUND));
    }

    @Transactional
    public Option subtractQuantity(Long id, int amount) {
        Option option = findById(id);
        option.subtractQuantity(amount);
        return optionRepository.save(option);
    }

    @Transactional
    public Option create(Long productId, String name, int quantity) {
        Product product = productService.findById(productId);

        if (optionRepository.existsByProductIdAndName(productId, name)) {
            throw new ApplicationException(OptionErrorCode.DUPLICATE_NAME);
        }

        return optionRepository.save(new Option(product, name, quantity));
    }

    @Transactional
    public void delete(Long productId, Long optionId) {
        Product product = productService.findById(productId);

        Option option = product.getOptions().stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(OptionErrorCode.NOT_FOUND));

        product.removeOption(option);
    }

}
