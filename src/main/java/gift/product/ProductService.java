package gift.product;

import gift.category.Category;
import gift.category.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다. id: " + id));
    }

    public Product create(String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        validateName(name, allowKakao);
        Category category = categoryService.findById(categoryId);
        return productRepository.save(new Product(name, price, imageUrl, category));
    }

    public Product update(Long id, String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        validateName(name, allowKakao);
        Product product = findById(id);
        Category category = categoryService.findById(categoryId);
        product.update(name, price, imageUrl, category);
        return productRepository.save(product);
    }

    private void validateName(String name, boolean allowKakao) {
        List<String> errors = ProductNameValidator.validate(name, allowKakao);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
