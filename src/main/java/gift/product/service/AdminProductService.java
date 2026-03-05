package gift.product.service;

import gift.category.entity.Category;
import gift.category.service.CategoryService;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    public List<Category> findAllCategories() {
        return categoryService.findAll();
    }

    @Transactional
    public void createProduct(String name, int price, String imageUrl, Long categoryId) {
        Category category = categoryService.findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId));
        productRepository.save(new Product(name, price, imageUrl, category));
    }

    public Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
    }

    @Transactional
    public void updateProduct(Long id, String name, int price, String imageUrl, Long categoryId) {
        Product product = findProductOrThrow(id);
        Category category = categoryService.findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId));

        product.update(name, price, imageUrl, category);
        productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
