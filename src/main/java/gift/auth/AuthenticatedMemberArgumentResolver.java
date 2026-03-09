package gift.auth;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import gift.member.Member;

@Component
public class AuthenticatedMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final AuthenticationResolver authenticationResolver;

    public AuthenticatedMemberArgumentResolver(AuthenticationResolver authenticationResolver) {
        this.authenticationResolver = authenticationResolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedMember.class)
            && Member.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Member resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        String authorization = webRequest.getHeader("Authorization");
        if (authorization == null) {
            throw new MissingAuthorizationException("Authorization 헤더가 필요합니다.");
        }

        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            throw new UnauthorizedException("유효하지 않은 인증 토큰입니다.");
        }

        return member;
    }
}
