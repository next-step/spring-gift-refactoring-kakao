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

    @Transactional
    public CategoryResponse createCategory(CategoryRequest createRequest) {
        Category newEntity = categoryRepo.save(createRequest.toEntity());

        return CategoryResponse.from(newEntity);
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest updateRequest) {
        Category find = categoryRepo.findById(categoryId)
                .orElseThrow(NotFoundException::categoryNotFound);

        String name = updateRequest.name();
        String color = updateRequest.color();
        String imageUlr = updateRequest.imageUrl();
        String description = updateRequest.description();

        find.update(name, color, imageUlr, description);

        return CategoryResponse.from(find);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category find = categoryRepo.findById(categoryId)
                .orElseThrow(NotFoundException::categoryNotFound);

        categoryRepo.delete(find);
    }
}
