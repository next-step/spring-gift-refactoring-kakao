package gift.auth;

import gift.TestFixtures;
import gift.member.Member;
import gift.member.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.Level;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthenticationResolverTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AuthenticationResolver resolver;

    @Test
    void extractMember_validToken_returnsMember() {
        var member = TestFixtures.member(1L, "test@test.com", "pw");
        given(jwtProvider.getEmail("valid-token")).willReturn("test@test.com");
        given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(member));

        var result = resolver.extractMember("Bearer valid-token");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void extractMember_invalidToken_returnsNull() {
        given(jwtProvider.getEmail("bad-token")).willThrow(new RuntimeException("invalid token"));

        var result = resolver.extractMember("Bearer bad-token");

        assertThat(result).isNull();
    }

    @Test
    void extractMember_memberNotFound_returnsNull() {
        given(jwtProvider.getEmail("valid-token")).willReturn("unknown@test.com");
        given(memberRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        var result = resolver.extractMember("Bearer valid-token");

        assertThat(result).isNull();
    }

    @Test
    void extractMember_invalidToken_logsWarning() {
        var loggerUnderTest = (Logger) LoggerFactory.getLogger(AuthenticationResolver.class);
        var listAppender = new ListAppender<ILoggingEvent>();
        listAppender.start();
        loggerUnderTest.addAppender(listAppender);

        given(jwtProvider.getEmail("bad-token")).willThrow(new RuntimeException("invalid token"));

        resolver.extractMember("Bearer bad-token");

        assertThat(listAppender.list)
            .anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().contains("인증"));
    }
}
