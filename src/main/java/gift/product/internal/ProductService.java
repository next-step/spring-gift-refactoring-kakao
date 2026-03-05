package gift.product.internal;

import gift.category.Category;
import gift.category.CategoryQueryPort;
import gift.global.NotFoundException;
import gift.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryQueryPort categoryQueryPort;

    public PagedModel<ProductResponse> getProducts(Long categoryId, Pageable pageable) {
        Page<Product> page = categoryId == null ?
                productRepo.findAll(pageable) :
                productRepo.findByCategoryId(categoryId, pageable);

        Page<ProductResponse> pageResponse = page.map(ProductResponse::from);

        return new PagedModel<>(pageResponse);
    }

    public ProductResponse getProduct(Long productId) {
        Product find = productRepo.findById(productId)
                .orElseThrow(NotFoundException::productNotFound);

        return ProductResponse.from(find);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest createRequest) {
        Long categoryId = createRequest.categoryId();

        Category category = categoryQueryPort.getReference(categoryId);

        String name = createRequest.name();
        int price = createRequest.price();
        String imageUrl = createRequest.imageUrl();

        Product build = Product.builder()
                .category(category)
                .name(name)
                .price(price)
                .imageUrl(imageUrl)
                .build();

        Product newEntity = productRepo.save(build);

        return ProductResponse.from(newEntity);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest updateRequest) {
        Long categoryId = updateRequest.categoryId();

        Category category = categoryQueryPort.getReference(categoryId);

        Product find = productRepo.findById(productId)
                .orElseThrow(NotFoundException::productNotFound);

        String name = updateRequest.name();
        int price = updateRequest.price();
        String imageUrl = updateRequest.imageUrl();

        find.update(name, price, imageUrl, category);

        return ProductResponse.from(find);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product find = productRepo.findById(productId)
                .orElseThrow(NotFoundException::productNotFound);

        productRepo.delete(find);
    }
}
