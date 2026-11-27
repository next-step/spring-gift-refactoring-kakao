package gift.category;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> findCategories() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    public Category createCategory(CategoryRequest request) {
        return categoryRepository.save(request.toEntity());
    }

    public Optional<Category> updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            return Optional.empty();
        }

        category.update(request.name(), request.color(), request.imageUrl(), request.description());
        categoryRepository.save(category);
        return Optional.of(category);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
