package gift.category;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category findByIdOrThrow(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

    @Transactional
    public void deleteById(Long id) {
        categoryRepository.deleteById(id);
    }
}
