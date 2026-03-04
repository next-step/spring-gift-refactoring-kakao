package gift.auth;

import gift.member.Member;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

public class MemberArgumentResolver implements HandlerMethodArgumentResolver {
    private final AuthenticationResolver authenticationResolver;

    public MemberArgumentResolver(AuthenticationResolver authenticationResolver) {
        this.authenticationResolver = authenticationResolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class)
            && Member.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        String authorization = webRequest.getHeader("Authorization");
        if (authorization == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return member;
    }
}
