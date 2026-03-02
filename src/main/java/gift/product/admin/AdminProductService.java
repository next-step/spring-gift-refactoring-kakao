package gift.product.admin;

import gift.category.Category;
import gift.product.Product;
import gift.product.admin.ProductDto.CategoryDto;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final AdminProductRepository productRepo;
    private final AdminProductCategoryRepository categoryRepo;

    public List<ProductDto> getAllProducts() {
        return productRepo.findAllInnerJoinFetchCategory().stream()
                .map(ProductDto::from)
                .toList();
    }

    public List<CategoryDto> getAllCategories() {
        return categoryRepo.findAll().stream()
                .map(CategoryDto::from)
                .toList();
    }

    @Transactional
    public void createProduct(
            String name, int price, String imageUrl, Long categoryId
    ) {
        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new NoSuchElementException(
                        "카테고리가 존재하지 않습니다. id=" + categoryId
                ));

        Product build = Product.builder()
                .name(name)
                .price(price)
                .imageUrl(imageUrl)
                .category(category)
                .build();

        productRepo.save(build);
    }

    public ProductDto getProduct(Long id) {
        Product find = productRepo.findByIdInnerJoinFetchCategory(id)
                .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));

        return ProductDto.from(find);
    }

    @Transactional
    public void updateProduct(
            Long productId, String name, int price, String imageUrl, Long categoryId
    ) {
        Product find = productRepo.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));

        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId));

        find.update(
                name, price, imageUrl, category
        );
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product find = productRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));

        productRepo.delete(find);
    }
}
