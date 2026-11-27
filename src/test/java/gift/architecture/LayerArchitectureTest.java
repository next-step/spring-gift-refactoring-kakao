package gift.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "gift", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerArchitectureTest {

    @ArchTest
    static final ArchRule repositoriesDoNotDependOnControllers = noClasses()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Controller");

    // ArchUnit의 that().A.or().B.and().C는 왼쪽부터 묶인다: (A 또는 B) 그리고 C.
    @ArchTest
    static final ArchRule controllersDoNotDependOnRepositories = noClasses()
            .that()
            .areAnnotatedWith(RestController.class)
            .or()
            .areAnnotatedWith(Controller.class)
            .and()
            .doNotHaveSimpleName("AdminProductController") // docs/work/05-service-layer/tasks.md '옮기지 않은 것' 참고, 4단계 이후
            .and()
            .doNotHaveSimpleName("AdminMemberController") // docs/work/05-service-layer/tasks.md '옮기지 않은 것' 참고, 4단계 이후
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule servicesDoNotDependOnWebLayer = noClasses()
            .that()
            .haveSimpleNameEndingWith("Service")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.http..", "org.springframework.web..", "org.springframework.ui..");
}
