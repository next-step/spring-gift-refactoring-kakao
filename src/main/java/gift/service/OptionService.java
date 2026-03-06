package gift.service;

import gift.common.NameValidator;
import gift.model.Option;
import gift.model.Product;
import gift.repository.OptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductService productService;

    public OptionService(OptionRepository optionRepository, ProductService productService) {
        this.optionRepository = optionRepository;
        this.productService = productService;
    }

    public List<Option> findByProductId(Long productId) {
        productService.findById(productId);
        return optionRepository.findByProductId(productId);
    }

    public Option findById(Long id) {
        return optionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Option not found. id=" + id));
    }

    public Option create(Long productId, String name, int quantity) {
        validateName(name);
        Product product = productService.findById(productId);
        if (optionRepository.existsByProductIdAndName(productId, name)) {
            throw new IllegalArgumentException("Option name already exists.");
        }
        return optionRepository.save(new Option(product, name, quantity));
    }

    public void delete(Long productId, Long optionId) {
        productService.findById(productId);

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("Cannot delete the last option of a product.");
        }

        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new NoSuchElementException("Option not found. id=" + optionId));
        if (!option.productId().equals(productId)) {
            throw new NoSuchElementException("Option not found. id=" + optionId);
        }

        optionRepository.delete(option);
    }

    public Option update(Long productId, Long optionId, String name, int quantity) {
        validateName(name);
        productService.findById(productId);

        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new NoSuchElementException("Option not found. id=" + optionId));
        if (!option.productId().equals(productId)) {
            throw new NoSuchElementException("Option not found. id=" + optionId);
        }
        if (optionRepository.existsByProductIdAndNameAndIdNot(productId, name, optionId)) {
            throw new IllegalArgumentException("Option name already exists.");
        }

        option.update(name, quantity);
        return optionRepository.save(option);
    }

    public Option subtractQuantity(Long optionId, int quantity) {
        Option option = optionRepository.findByIdForUpdate(optionId)
            .orElseThrow(() -> new NoSuchElementException("Option not found. id=" + optionId));
        option.subtractQuantity(quantity);
        return optionRepository.save(option);
    }

    private void validateName(String name) {
        List<String> errors = NameValidator.validate(name, "Option name", 50);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
