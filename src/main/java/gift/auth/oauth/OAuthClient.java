package gift.auth.oauth;

import gift.external.ExternalProvider;

import java.net.URI;

public interface OAuthClient {
    ExternalProvider provider();

    URI getLoginUri();

    OAuthUserInfo getUserInfo(String code);
}
