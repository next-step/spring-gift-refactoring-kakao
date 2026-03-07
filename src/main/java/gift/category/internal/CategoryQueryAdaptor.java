package gift.category.internal;

import gift.category.Category;
import gift.category.CategoryDto;
import gift.category.CategoryQueryPort;
import gift.global.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CategoryQueryAdaptor implements CategoryQueryPort {

    private final CategoryRepository categoryRepo;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Category getReference(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(NotFoundException::categoryNotFound);
    }

    @Override
    public List<CategoryDto> findAll() {
        return categoryRepo.findAll().stream()
                .map(CategoryQueryAdaptor::convertToDto)
                .toList();
    }

    private static CategoryDto convertToDto(Category entity) {
        Long id = entity.getId();
        String name = entity.getName();
        String color = entity.getColor();
        String imageUrl = entity.getImageUrl();
        String description = entity.getDescription();

        return new CategoryDto(id, name, color, imageUrl, description);
    }
}
