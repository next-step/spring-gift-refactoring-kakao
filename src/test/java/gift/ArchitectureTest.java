package gift;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

class ArchitectureTest {

    private static final JavaClasses classes = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("gift");

    @Test
    void Service_클래스는_Service_어노테이션이_있어야_한다() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Service")
            .should().beAnnotatedWith(Service.class);

        rule.check(classes);
    }

    @Test
    void Service_클래스는_Transactional_readOnly_어노테이션이_있어야_한다() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Service")
            .should().beAnnotatedWith(Transactional.class);

        rule.check(classes);
    }

    @Test
    void Autowired_필드_주입은_사용할_수_없다() {
        ArchRule rule = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .because("생성자 주입을 사용해야 합니다");

        rule.check(classes);
    }

    @Test
    void Service_클래스의_필드는_private_final이어야_한다() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Service")
            .should().bePrivate()
            .andShould().beFinal()
            .because("의존성은 불변이어야 합니다");

        rule.check(classes);
    }
}
