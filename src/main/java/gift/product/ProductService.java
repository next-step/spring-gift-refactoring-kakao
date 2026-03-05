package gift.product;

import gift.category.Category;
import gift.category.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
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

    @Transactional
    public Product create(String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        validateKakaoPolicy(name, allowKakao);
        Category category = categoryService.findById(categoryId);
        return productRepository.save(new Product(name, price, imageUrl, category));
    }

    @Transactional
    public Product update(Long id, String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        validateKakaoPolicy(name, allowKakao);
        Product product = findById(id);
        Category category = categoryService.findById(categoryId);
        product.update(name, price, imageUrl, category);
        return productRepository.save(product);
    }

    private void validateKakaoPolicy(String name, boolean allowKakao) {
        if (!allowKakao && name != null && name.contains("카카오")) {
            throw new IllegalArgumentException("\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.");
        }
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
