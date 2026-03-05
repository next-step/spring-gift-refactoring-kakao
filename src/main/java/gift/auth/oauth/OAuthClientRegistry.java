package gift.auth.oauth;

import gift.external.ExternalProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuthClientRegistry {
    private final Map<ExternalProvider, OAuthClient> clients;

    public OAuthClientRegistry(List<OAuthClient> clients) {
        this.clients = clients.stream()
            .collect(Collectors.toMap(OAuthClient::provider, Function.identity()));
    }

    public OAuthClient get(ExternalProvider provider) {
        OAuthClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalStateException("OAuth client not found. provider=" + provider);
        }
        return client;
    }
}
