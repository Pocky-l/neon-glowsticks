package com.pockyl.neon_glowsticks.light;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/** Which glowstick lights are shown when there are more than the configured maximum. */
public final class LightPriority {
    private LightPriority() {
    }

    /**
     * The at most {@code max} sources that give light, most important first: the player's own sources (a glowstick in
     * their hand) before all others, then the nearest ones. The order also decides which lights are rebuilt first.
     */
    public static <T> List<T> select(List<T> sources, Predicate<T> own, ToDoubleFunction<T> distanceSqr, int max) {
        List<T> sorted = new ArrayList<>(sources);
        sorted.sort(Comparator.comparing((T source) -> !own.test(source)).thenComparingDouble(distanceSqr));
        return sorted.size() > max ? sorted.subList(0, Math.max(0, max)) : sorted;
    }
}
