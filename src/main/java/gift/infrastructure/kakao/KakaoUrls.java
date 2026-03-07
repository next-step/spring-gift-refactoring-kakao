package gift.infrastructure.kakao;

public final class KakaoUrls {
  public static final String AUTHORIZE = "https://kauth.kakao.com/oauth/authorize";
  public static final String TOKEN = "https://kauth.kakao.com/oauth/token";
  public static final String USER_INFO = "https://kapi.kakao.com/v2/user/me";
  public static final String SEND_MESSAGE = "https://kapi.kakao.com/v2/api/talk/memo/default/send";

  private KakaoUrls() {}
}
