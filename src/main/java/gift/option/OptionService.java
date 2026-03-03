package gift.option;

import gift.product.Product;
import gift.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class OptionService {
    private final ProductRepository productRepository;

    public OptionService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<OptionResponse> findByProductId(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("Product not found. id=" + productId));
        return product.getOptions().stream()
            .map(OptionResponse::from)
            .toList();
    }

    public OptionResponse create(Long productId, OptionRequest request) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("Product not found. id=" + productId));

        product.addOption(request.name(), request.quantity());
        Product saved = productRepository.save(product);
        Option created = saved.getOptions().stream()
            .filter(o -> o.getName().equals(request.name()))
            .findFirst()
            .orElseThrow();
        return OptionResponse.from(created);
    }

    public void delete(Long productId, Long optionId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("Product not found. id=" + productId));

        Option option = product.getOptions().stream()
            .filter(o -> o.getId().equals(optionId))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Option not found. id=" + optionId));

        product.removeOption(option);
        productRepository.save(product);
    }
}
