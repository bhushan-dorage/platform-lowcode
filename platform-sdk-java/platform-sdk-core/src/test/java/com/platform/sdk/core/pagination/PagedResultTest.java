package com.platform.sdk.core.pagination;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

class PagedResultTest {

    @Test
    void singlePage_iteratesAllItems() {
        PagedResult<String> page = new PagedResult<>(List.of("a", "b", "c"), null, false, cursor -> {
            throw new RuntimeException("should not fetch next page");
        });
        assertThat(page).containsExactly("a", "b", "c");
    }

    @Test
    void multiplePages_lazyFetches() {
        PagedResult<String> page2 = new PagedResult<>(List.of("c", "d"), null, false, cursor -> null);
        PagedResult<String> page1 = new PagedResult<>(List.of("a", "b"), "cursor-2", true, cursor -> page2);
        assertThat(page1).containsExactly("a", "b", "c", "d");
    }

    @Test
    void emptyPage_hasNoElements() {
        PagedResult<String> page = new PagedResult<>(List.of(), null, false, cursor -> null);
        assertThat(page).isEmpty();
    }

    @Test
    void getContent_returnsFirstPage() {
        PagedResult<Integer> page = new PagedResult<>(List.of(1, 2), "cur", true, c -> null);
        assertThat(page.getContent()).containsExactly(1, 2);
        assertThat(page.hasMore()).isTrue();
    }
}
