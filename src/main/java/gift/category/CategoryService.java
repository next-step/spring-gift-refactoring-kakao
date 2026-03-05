package gift.category;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("카테고리를 찾을 수 없습니다. id: " + id));
    }

    @Transactional
    public Category create(String name, String color, String imageUrl, String description) {
        return categoryRepository.save(new Category(name, color, imageUrl, description));
    }

    @Transactional
    public Category update(Long id, String name, String color, String imageUrl, String description) {
        Category category = findById(id);
        category.update(name, color, imageUrl, description);
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }
}
