package gift;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "gift", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest
  static final ArchRule Controller는_Controller로_끝나야_한다 =
      classes()
          .that()
          .areAnnotatedWith(Controller.class)
          .or()
          .areAnnotatedWith(RestController.class)
          .should()
          .haveSimpleNameEndingWith("Controller");

  @ArchTest
  static final ArchRule Service는_Service로_끝나야_한다 =
      classes()
          .that()
          .areAnnotatedWith(Service.class)
          .should()
          .haveSimpleNameEndingWith("Service");

  @ArchTest
  static final ArchRule Repository는_Repository로_끝나야_한다 =
      classes()
          .that()
          .areAssignableTo(JpaRepository.class)
          .should()
          .haveSimpleNameEndingWith("Repository");

  @ArchTest
  static final ArchRule RestController는_Repository를_직접_사용하지_않는다 =
      noClasses()
          .that()
          .areAnnotatedWith(RestController.class)
          .should()
          .dependOnClassesThat()
          .areAssignableTo(JpaRepository.class);
}
