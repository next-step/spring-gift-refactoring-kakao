package gift.product.admin;

import gift.category.Category;
import gift.category.CategoryQueryPort;
import gift.global.NotFoundException;
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
    private final CategoryQueryPort categoryQueryPort;

    public List<ProductDto> getAllProducts() {
        return productRepo.findAllInnerJoinFetchCategory().stream()
                .map(ProductDto::from)
                .toList();
    }

    public List<CategoryDto> getAllCategories() {
        return categoryQueryPort.findAll().stream()
                .map(c -> new CategoryDto(c.id(), c.name()))
                .toList();
    }

    @Transactional
    public void createProduct(
            String name, int price, String imageUrl, Long categoryId
    ) {
        Category category = getCategoryOrThrow(categoryId);

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

    private Category getCategoryOrThrow(Long categoryId) {
        try {
            return categoryQueryPort.getReference(categoryId);
        } catch (NotFoundException e) {
            throw new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId);
        }
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product find = productRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));

        productRepo.delete(find);
    }

    @Transactional
    public void updateProduct(
            Long productId, String name, int price, String imageUrl, Long categoryId
    ) {
        Product find = productRepo.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));

        Category category = getCategoryOrThrow(categoryId);

        find.update(
                name, price, imageUrl, category
        );
    }
}
