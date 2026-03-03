package gift.option;

import gift.product.Product;
import gift.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    @Autowired
    public OptionService(OptionRepository optionRepository, ProductRepository productRepository) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<OptionResponse> findByProductId(Long productId) {
        productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다. id=" + productId));
        return optionRepository.findByProductId(productId).stream()
            .map(OptionResponse::from)
            .toList();
    }

    @Transactional
    public OptionResponse create(Long productId, OptionRequest request) {
        List<String> errors = OptionNameValidator.validate(request.name());
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다. id=" + productId));

        if (optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        Option saved = optionRepository.save(request.toEntity(product));
        return OptionResponse.from(saved);
    }

    @Transactional
    public void delete(Long productId, Long optionId) {
        productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다. id=" + productId));

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new NoSuchElementException("옵션을 찾을 수 없습니다. id=" + optionId));

        if (!option.getProduct().getId().equals(productId)) {
            throw new NoSuchElementException("해당 상품의 옵션이 아닙니다. optionId=" + optionId);
        }

        optionRepository.delete(option);
    }
}
