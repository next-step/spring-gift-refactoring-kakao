package gift.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "gift", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    @ArchTest
    static final ArchRule controllers_should_not_depend_on_repositories = noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Repository")
            .because("Controller는 Service를 통해서만 데이터에 접근해야 한다");

    @ArchTest
    static final ArchRule services_should_not_depend_on_controllers = noClasses()
            .that()
            .haveSimpleNameEndingWith("Service")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Controller")
            .because("Service는 Controller에 의존하지 않아야 한다");

    @ArchTest
    static final ArchRule repositories_should_not_depend_on_services = noClasses()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Service")
            .because("Repository는 Service에 의존하지 않아야 한다");

    @ArchTest
    static final ArchRule repositories_should_not_depend_on_controllers = noClasses()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Controller")
            .because("Repository는 Controller에 의존하지 않아야 한다");
}
