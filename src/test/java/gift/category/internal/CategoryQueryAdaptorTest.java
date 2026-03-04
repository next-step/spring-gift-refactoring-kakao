package gift.category.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gift.category.Category;
import gift.category.CategoryQueryPort;
import gift.global.NotFoundException;
import gift.support.TestCategoryRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(CategoryQueryAdaptor.class)
class CategoryQueryAdaptorTest {

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    @Autowired
    CategoryQueryPort categoryQueryPort;

    @Autowired
    TestCategoryRepository testCategoryRepo;

    @Autowired
    TransactionTemplate transactionTemplate;

    @AfterEach
    void tearDown() {
        testCategoryRepo.deleteAllInBatch();
    }

    @Test
    @DisplayName("존재하는 카테고리를 참조한다")
    void testGetReference() {
        // given
        Category saved = createCategory("교환권", "#000000", "http://img", "설명");

        // when
        Category result = transactionTemplate.execute(
                status -> categoryQueryPort.getReference(saved.getId())
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getName()).isEqualTo("교환권");
    }

    private Category createCategory(
            String name, String color,
            String imageUrl, String description
    ) {
        Category newEntity = Category.builder()
                .name(name)
                .color(color)
                .imageUrl(imageUrl)
                .description(description)
                .build();

        return testCategoryRepo.save(newEntity);
    }

    @Test
    @DisplayName("존재하지 않는 카테고리를 참조하면 NotFoundException 이 발생한다")
    void testGetReferenceNotFound() {
        // when + then
        assertThatThrownBy(() ->
                transactionTemplate.execute(
                        status -> categoryQueryPort.getReference(NOT_EXISTING_ID)
                )
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("트랜잭션 없이 getReference 를 호출하면 IllegalTransactionStateException 이 발생한다")
    void testGetReferenceWithoutTransaction() {
        // given
        Category saved = createCategory("교환권", "#000000", "http://img", "설명");

        // when + then
        assertThatThrownBy(() -> categoryQueryPort.getReference(saved.getId()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    @DisplayName("전체 카테고리를 조회한다")
    void testFindAll() {
        // given
        createCategory("교환권", "#000000", "http://img1", "설명1");
        createCategory("상품권", "#FFFFFF", "http://img2", "설명2");

        // when
        List<Category> result = categoryQueryPort.findAll();

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("카테고리가 없으면 빈 목록을 반환한다")
    void testFindAllEmpty() {
        // when
        List<Category> result = categoryQueryPort.findAll();

        // then
        assertThat(result).isEmpty();
    }
}
