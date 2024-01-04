package net.stracciatella.injected;

public interface StracciatellaInjections {
    void initializeMixins();

    class Holder {
        public static StracciatellaInjections injections;

        @SuppressWarnings("unused") // Called from ClassDelegateTransformer
        public static void initializeMixins() {
            injections.initializeMixins();
        }
    }
}
