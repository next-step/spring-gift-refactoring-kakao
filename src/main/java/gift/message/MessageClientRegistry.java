package gift.message;

import gift.external.ExternalProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MessageClientRegistry {
    private final Map<ExternalProvider, MessageClient> clients;

    public MessageClientRegistry(List<MessageClient> clients) {
        this.clients = clients.stream()
            .collect(Collectors.toMap(MessageClient::provider, Function.identity()));
    }

    public MessageClient get(ExternalProvider provider) {
        MessageClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalStateException("Message client not found. provider=" + provider);
        }
        return client;
    }
}
