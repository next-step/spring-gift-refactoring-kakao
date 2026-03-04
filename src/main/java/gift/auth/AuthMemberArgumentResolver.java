package gift.auth;

import gift.member.Member;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthMemberArgumentResolver implements HandlerMethodArgumentResolver {
    private final AuthenticationResolver authenticationResolver;

    public AuthMemberArgumentResolver(AuthenticationResolver authenticationResolver) {
        this.authenticationResolver = authenticationResolver;
    }

    // @AuthMember가 붙은 Member 타입 파라미터에만 동작
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthMember.class)
                && Member.class.isAssignableFrom(parameter.getParameterType());
    }

    // Authorization 헤더에서 토큰을 꺼내 회원을 찾고, 실패 시 401 응답
    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory)
            throws Exception {
        final String authorization = webRequest.getHeader("Authorization");
        final Member member = (authorization != null) ? authenticationResolver.extractMember(authorization) : null;

        if (member == null) {
            final HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mavContainer.setRequestHandled(true);
            return null;
        }

        return member;
    }
}
