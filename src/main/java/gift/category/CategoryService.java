package gift.category;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category create(CategoryRequest request) {
        return categoryRepository.save(request.toEntity());
    }

    public Optional<Category> update(Long id, CategoryRequest request) {
        return categoryRepository.findById(id)
            .map(category -> {
                category.update(request.name(), request.color(), request.imageUrl(), request.description());
                return categoryRepository.save(category);
            });
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }
}
