package net.stracciatella.testing.command;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Intercepts incoming chat/system messages for test assertions.
 * The {@link net.stracciatella.testing.mixin.ChatListenerMixin} feeds messages here.
 */
public class ChatInterceptor {
    private static final ChatInterceptor INSTANCE = new ChatInterceptor();

    private final List<String> messages = new CopyOnWriteArrayList<>();
    private Consumer<String> listener;

    private ChatInterceptor() {
    }

    public static ChatInterceptor instance() {
        return INSTANCE;
    }

    /**
     * Called by the mixin when a system message is received.
     */
    public void onMessage(String message) {
        messages.add(message);
        if (listener != null) {
            listener.accept(message);
        }
    }

    /**
     * Set a listener that fires on every incoming message.
     */
    public void setListener(Consumer<String> listener) {
        this.listener = listener;
    }

    /**
     * Clear the listener.
     */
    public void clearListener() {
        this.listener = null;
    }

    /**
     * Get all messages captured since last clear.
     */
    public List<String> messages() {
        return List.copyOf(messages);
    }

    /**
     * Check if any captured message contains the given substring.
     */
    public boolean hasMessageContaining(String substring) {
        return messages.stream().anyMatch(m -> m.contains(substring));
    }

    /**
     * Clear captured messages.
     */
    public void clear() {
        messages.clear();
        listener = null;
    }
}
