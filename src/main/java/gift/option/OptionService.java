package gift.option;

import gift.product.Product;
import gift.product.ProductService;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OptionService {
  private static final int MIN_OPTION_COUNT = 1;

  private final OptionRepository optionRepository;
  private final ProductService productService;

  public OptionService(OptionRepository optionRepository, ProductService productService) {
    this.optionRepository = optionRepository;
    this.productService = productService;
  }

  public List<Option> findByProductId(Long productId) {
    productService
        .findById(productId)
        .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));
    return optionRepository.findByProductId(productId);
  }

  @Transactional
  public Option create(Long productId, String name, int quantity) {
    validateNameOrThrow(name);
    Product product =
        productService
            .findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));

    if (optionRepository.existsByProductIdAndName(productId, name)) {
      throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
    }

    return optionRepository.save(new Option(product, name, quantity));
  }

  @Transactional
  public boolean delete(Long productId, Long optionId) {
    productService
        .findById(productId)
        .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));

    List<Option> options = optionRepository.findByProductId(productId);
    if (options.size() <= MIN_OPTION_COUNT) {
      throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
    }

    Option option = optionRepository.findById(optionId).orElse(null);
    if (option == null || !option.getProduct().getId().equals(productId)) {
      return false;
    }

    optionRepository.delete(option);
    return true;
  }

  private void validateNameOrThrow(String name) {
    List<String> errors = OptionNameValidator.validate(name);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(String.join(", ", errors));
    }
  }
}
