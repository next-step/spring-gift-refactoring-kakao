package gift.auth;

import gift.exception.UnauthorizedException;
import gift.member.Member;
import gift.member.MemberService;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthenticationResolver implements HandlerMethodArgumentResolver {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final MemberService memberService;

    public AuthenticationResolver(JwtProvider jwtProvider, MemberService memberService) {
        this.jwtProvider = jwtProvider;
        this.memberService = memberService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Login.class)
            && Member.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Member resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String authorization = webRequest.getHeader("Authorization");
        return extractMember(authorization);
    }

    private Member extractMember(String authorization) {
        try {
            String token = authorization.replace(BEARER_PREFIX, "");
            String email = jwtProvider.getEmail(token);
            return memberService.getMemberByEmail(email);
        } catch (Exception e) {
            throw new UnauthorizedException("유효하지 않은 인증 정보입니다.");
        }
    }
}
