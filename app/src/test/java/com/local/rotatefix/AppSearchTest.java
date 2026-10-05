package com.local.rotatefix;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppSearchTest {
    @Test
    public void emptyQueryMatchesEveryApp() {
        assertTrue(AppSearch.matches("YouTube", "com.google.android.youtube", "  "));
    }

    @Test
    public void matchingIgnoresCaseAndOuterWhitespace() {
        assertTrue(AppSearch.matches("YouTube", "com.google.android.youtube", "  tube "));
        assertTrue(AppSearch.matches("YouTube", "com.google.android.youtube", " GOOGLE.Android "));
    }

    @Test
    public void unrelatedQueryDoesNotMatch() {
        assertFalse(AppSearch.matches("YouTube", "com.google.android.youtube", "camera"));
    }
}
