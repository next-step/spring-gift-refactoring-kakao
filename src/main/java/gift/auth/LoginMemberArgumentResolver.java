package gift.auth;

import gift.member.Member;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final AuthenticationResolver authenticationResolver;

    public LoginMemberArgumentResolver(AuthenticationResolver authenticationResolver) {
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
        var authorization = webRequest.getHeader("Authorization");
        if (authorization == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Required request header 'Authorization' is not present");
        }

        var member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return member;
    }
}
