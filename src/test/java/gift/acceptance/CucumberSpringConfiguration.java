package gift.acceptance;

import gift.Application;
import gift.auth.KakaoLoginClient;
import gift.order.KakaoMessageClient;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SuppressWarnings("unused")
@ActiveProfiles("acceptance-test")
@CucumberContextConfiguration
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {

    @MockitoBean
    private KakaoMessageClient kakaoMessageClient;

    @MockitoBean
    private KakaoLoginClient kakaoLoginClient;
}
