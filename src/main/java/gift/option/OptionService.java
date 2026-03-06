package gift.option;

import gift.error.DuplicateException;
import gift.error.EntityNotFoundException;
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

    public List<Option> getOptions(Long productId) {
        findProduct(productId);
        return optionRepository.findByProductId(productId);
    }

    @Transactional
    public Option createOption(Long productId, OptionRequest request) {
        Product product = findProduct(productId);

        if (optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new DuplicateException("이미 존재하는 옵션명입니다.");
        }

        return optionRepository.save(new Option(product, request.name(), request.quantity()));
    }

    @Transactional
    public void deleteOption(Long productId, Long optionId) {
        findProduct(productId);

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new EntityNotFoundException("옵션이 존재하지 않습니다."));
        option.validateBelongsTo(productId);

        optionRepository.delete(option);
    }

    @Transactional
    public Option updateOption(Long productId, Long optionId, OptionRequest request) {
        findProduct(productId);

        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new EntityNotFoundException("옵션이 존재하지 않습니다."));
        option.validateBelongsTo(productId);

        if (optionRepository.existsByProductIdAndNameAndIdNot(productId, request.name(), optionId)) {
            throw new DuplicateException("이미 존재하는 옵션명입니다.");
        }

        option.update(request.name(), request.quantity());
        return optionRepository.save(option);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("상품이 존재하지 않습니다."));
    }
}
