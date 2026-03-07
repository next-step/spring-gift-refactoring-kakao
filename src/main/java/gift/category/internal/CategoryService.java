package gift.category.internal;

import gift.category.Category;
import gift.global.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepo;

    public List<CategoryResponse> getCategories() {
        return categoryRepo.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse getCategory(Long categoryId) {
        Category find = categoryRepo.findById(categoryId)
                .orElseThrow(NotFoundException::categoryNotFound);

        return CategoryResponse.from(find);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest createRequest) {
        String name = createRequest.name();
        String color = createRequest.color();
        String imageUrl = createRequest.imageUrl();
        String description = createRequest.description();

        Category build = Category.builder()
                .name(name)
                .color(color)
                .imageUrl(imageUrl)
                .description(description)
                .build();

        Category newEntity = categoryRepo.save(build);

        return CategoryResponse.from(newEntity);
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest updateRequest) {
        Category find = categoryRepo.findById(categoryId)
                .orElseThrow(NotFoundException::categoryNotFound);

        String name = updateRequest.name();
        String color = updateRequest.color();
        String imageUrl = updateRequest.imageUrl();
        String description = updateRequest.description();

        find.update(name, color, imageUrl, description);

        return CategoryResponse.from(find);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category find = categoryRepo.findById(categoryId)
                .orElseThrow(NotFoundException::categoryNotFound);

        categoryRepo.delete(find);
    }
}
