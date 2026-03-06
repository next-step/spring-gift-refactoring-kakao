package gift.category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
            .map(CategoryResponse::from)
            .toList();
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        Category saved = categoryRepository.save(request.toEntity());
        return CategoryResponse.from(saved);
    }

    public Optional<CategoryResponse> updateCategory(Long id, CategoryRequest request) {
        return categoryRepository.findById(id)
            .map(category -> {
                category.update(request.name(), request.color(), request.imageUrl(), request.description());
                categoryRepository.save(category);
                return CategoryResponse.from(category);
            });
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    // Admin operations

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다. id=" + id));
    }
}
