package chat.tamtam.bot.webhook;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chat.tamtam.bot.TamTamBot;
import chat.tamtam.bot.TamTamBotBase;
import chat.tamtam.bot.exceptions.TamTamBotException;
import chat.tamtam.botapi.client.TamTamClient;
import chat.tamtam.botapi.exceptions.APIException;
import chat.tamtam.botapi.exceptions.ClientException;
import chat.tamtam.botapi.model.Subscription;
import chat.tamtam.botapi.model.SubscriptionRequestBody;
import chat.tamtam.botapi.queries.GetSubscriptionsQuery;
import chat.tamtam.botapi.queries.SubscribeQuery;
import chat.tamtam.botapi.queries.UnsubscribeQuery;

/**
 * @author alexandrchuprin
 */
public abstract class WebhookBot extends TamTamBotBase implements TamTamBot {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final WebhookBotOptions options;

    public WebhookBot(TamTamClient client, WebhookBotOptions options, Object... handlers) {
        super(client, handlers);
        this.options = options;
    }

    /**
     * @return registered webhook URL
     */
    public String start(WebhookBotContainer container) throws TamTamBotException {
        if (options.shouldRemoveOldSubscriptions()) {
            try {
                unsubscribe();
            } catch (APIException | ClientException e) {
                LOG.warn("Failed to remove current subscriptions", e);
            }
        }

        String webhookUrl = container.getWebhookUrl(this);
        SubscriptionRequestBody body = new SubscriptionRequestBody(webhookUrl);
        body.updateTypes(options.getUpdateTypes());

        try {
            new SubscribeQuery(getClient(), body).execute();
        } catch (APIException | ClientException e) {
            throw new TamTamBotException("Failed to start webhook bot", e);
        }

        return webhookUrl;
    }

    public void stop(WebhookBotContainer container) {
        // do nothing by default
    }

    /**
     * Should return unique key across all bots in container. Returns access token by default.
     */
    public String getKey() {
        return getClient().getAccessToken();
    }

    private void unsubscribe() throws APIException, ClientException {
        List<Subscription> subscriptions = new GetSubscriptionsQuery(getClient()).execute().getSubscriptions();
        for (Subscription subscription : subscriptions) {
            new UnsubscribeQuery(getClient(), subscription.getUrl()).execute();
        }
    }
}
