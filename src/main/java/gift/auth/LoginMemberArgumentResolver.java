package gift.auth;

import gift.member.Member;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

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
  public Member resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    String authorization = webRequest.getHeader("Authorization");
    if (authorization == null) {
      throw new UnauthorizedException("인증 헤더가 없습니다");
    }
    Member member = authenticationResolver.extractMember(authorization);
    if (member == null) {
      throw new UnauthorizedException("유효하지 않은 인증 정보입니다");
    }
    return member;
  }
}
