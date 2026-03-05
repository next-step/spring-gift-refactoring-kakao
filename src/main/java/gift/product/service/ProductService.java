package gift.product.service;

import gift.category.entity.Category;
import gift.category.service.CategoryService;
import gift.product.dto.ProductRequest;
import gift.product.dto.ProductResponse;
import gift.product.entity.Product;
import gift.product.exception.ProductErrorCode;
import gift.product.exception.ProductException;
import gift.product.repository.ProductRepository;
import gift.product.validation.ProductNameValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public Page<ProductResponse> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductResponse::from);
    }

    public ProductResponse getProduct(Long id) {
        Product product = findByIdOrThrow(id);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        ProductNameValidator.validateOrThrow(request.name());
        Category category = categoryService.findById(request.categoryId())
            .orElseThrow(() -> new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND));
        Product saved = productRepository.save(request.toEntity(category));
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        ProductNameValidator.validateOrThrow(request.name());
        Category category = categoryService.findById(request.categoryId())
            .orElseThrow(() -> new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND));
        Product product = findByIdOrThrow(id);
        product.update(request.name(), request.price(), request.imageUrl(), category);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public Product findByIdOrThrow(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }
}
