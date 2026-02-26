package gift.category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category createCategory(CategoryRequest request) {
        return categoryRepository.save(request.toEntity());
    }

    @Transactional
    public Category updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + id));
        category.update(request.name(), request.color(), request.imageUrl(), request.description());
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
