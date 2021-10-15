package net.minecraft.server.network;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ITextFilter {
    ITextFilter DUMMY = new ITextFilter() {
        @Override
        public void join() {
        }

        @Override
        public void leave() {
        }

        @Override
        public CompletableFuture<ITextFilter.FilteredText> processStreamMessage(String text) {
            return CompletableFuture.completedFuture(ITextFilter.FilteredText.passThrough(text));
        }

        @Override
        public CompletableFuture<List<ITextFilter.FilteredText>> processMessageBundle(List<String> texts) {
            return CompletableFuture.completedFuture(texts.stream().map(ITextFilter.FilteredText::passThrough).collect(ImmutableList.toImmutableList()));
        }
    };

    void join();

    void leave();

    CompletableFuture<ITextFilter.FilteredText> processStreamMessage(String text);

    CompletableFuture<List<ITextFilter.FilteredText>> processMessageBundle(List<String> texts);

    public static class FilteredText {
        public static final ITextFilter.FilteredText EMPTY = new ITextFilter.FilteredText("", "");
        private final String raw;
        private final String filtered;

        public FilteredText(String raw, String filtered) {
            this.raw = raw;
            this.filtered = filtered;
        }

        public String getRaw() {
            return this.raw;
        }

        public String getFiltered() {
            return this.filtered;
        }

        public static ITextFilter.FilteredText passThrough(String text) {
            return new ITextFilter.FilteredText(text, text);
        }

        public static ITextFilter.FilteredText fullyFiltered(String raw) {
            return new ITextFilter.FilteredText(raw, "");
        }
    }
}
