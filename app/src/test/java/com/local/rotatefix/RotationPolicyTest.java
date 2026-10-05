package com.local.rotatefix;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RotationPolicyTest {
    @Test
    public void portraitIsRequestedOnlyForSelectedPackage() {
        Set<String> selected = Set.of("com.example.reader");
        assertTrue(RotationPolicy.shouldForcePortrait("com.example.reader", selected));
        assertFalse(RotationPolicy.shouldForcePortrait("com.example.video", selected));
        assertFalse(RotationPolicy.shouldForcePortrait(null, selected));
    }

    @Test
    public void commonSystemWindowsAreTransient() {
        assertTrue(RotationPolicy.isTransientSystemUi("com.android.systemui", "ime.package"));
        assertTrue(RotationPolicy.isTransientSystemUi("ime.package", "ime.package"));
        assertFalse(RotationPolicy.isTransientSystemUi("com.example.other", "ime.package"));
    }
}
