package gift.acceptance.suite;

import org.junit.platform.suite.api.SelectClasspathResource;

@SelectClasspathResource("features/member.feature")
public class MemberCucumberFeatureTest extends AbstractCucumberFeatureTest {

}
