package com.platform.sdk.core.pagination;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class PagedResult<T> implements Iterable<T> {

    private final List<T> firstPage;
    private final String nextCursor;
    private final boolean hasMore;
    private final Function<String, PagedResult<T>> nextPageFetcher;

    public PagedResult(List<T> firstPage, String nextCursor, boolean hasMore,
                       Function<String, PagedResult<T>> nextPageFetcher) {
        this.firstPage = firstPage;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
        this.nextPageFetcher = nextPageFetcher;
    }

    public List<T> getContent() { return firstPage; }
    public boolean hasMore() { return hasMore; }
    public String getCursor() { return nextCursor; }

    @Override
    public Iterator<T> iterator() {
        return new PageIterator<>(this);
    }

    private static class PageIterator<T> implements Iterator<T> {
        private PagedResult<T> currentPage;
        private int index = 0;

        PageIterator(PagedResult<T> first) { this.currentPage = first; }

        @Override
        public boolean hasNext() {
            if (index < currentPage.getContent().size()) return true;
            if (!currentPage.hasMore()) return false;
            currentPage = currentPage.nextPageFetcher.apply(currentPage.getCursor());
            index = 0;
            return !currentPage.getContent().isEmpty();
        }

        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            return currentPage.getContent().get(index++);
        }
    }
}
