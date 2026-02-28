package gift.acceptance.suite;

import org.junit.platform.suite.api.SelectClasspathResource;

@SelectClasspathResource("features/auth.feature")
public class AuthCucumberFeatureTest extends AbstractCucumberFeatureTest {

}
