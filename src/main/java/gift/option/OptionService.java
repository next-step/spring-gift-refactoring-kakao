package gift.option;

import gift.product.Product;
import gift.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductService productService;

    public List<Option> findByProductId(Long productId) {
        productService.findById(productId);
        return optionRepository.findByProductId(productId);
    }

    public Option findById(Long id) {
        return optionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("옵션을 찾을 수 없습니다. id: " + id));
    }

    public Option create(Long productId, String name, int quantity) {
        validateName(name);
        Product product = productService.findById(productId);

        if (optionRepository.existsByProductIdAndName(productId, name)) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        return optionRepository.save(new Option(product, name, quantity));
    }

    public void delete(Long productId, Long optionId) {
        productService.findById(productId);

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        Option option = optionRepository.findById(optionId)
                .filter(o -> o.getProduct().getId().equals(productId))
                .orElseThrow(() -> new NoSuchElementException("옵션을 찾을 수 없습니다. id: " + optionId));

        optionRepository.delete(option);
    }

    private void validateName(String name) {
        List<String> errors = OptionNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
