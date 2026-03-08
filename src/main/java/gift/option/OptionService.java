package gift.option;

import gift.product.Product;
import gift.product.ProductRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        findProduct(productId);
        return optionRepository.findByProductId(productId);
    }

    @Transactional
    public Option create(Long productId, String name, int quantity) {
        OptionNameValidator.validateOrThrow(name);
        Product product = findProduct(productId);

        if (optionRepository.existsByProductIdAndName(productId, name)) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        return optionRepository.save(new Option(product, name, quantity));
    }

    @Transactional
    public void delete(Long productId, Long optionId) {
        findProduct(productId);

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        Option option = options.stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("해당 상품의 옵션이 아닙니다. optionId=" + optionId));

        optionRepository.delete(option);
    }

    private Product findProduct(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));
    }
}
