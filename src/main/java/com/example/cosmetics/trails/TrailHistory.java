package com.example.cosmetics.trails;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Fixed-length ring buffer of the player's recent positions.
 * Used by {@link com.example.cosmetics.render.TrailRenderer} to build a
 * continuous 3D ribbon behind the player instead of emitting particles.
 */
public final class TrailHistory {

    public static final int MAX_POINTS = 36;

    public static final class Point {
        public final double x, y, z;
        public final long timeMs;
        public Point(double x, double y, double z, long timeMs) {
            this.x = x; this.y = y; this.z = z; this.timeMs = timeMs;
        }
    }

    private static final Deque<Point> POINTS = new ArrayDeque<>(MAX_POINTS + 1);
    private static double lastX, lastY, lastZ;
    private static boolean hasLast = false;

    public static Deque<Point> points() { return POINTS; }

    public static void reset() {
        POINTS.clear();
        hasLast = false;
    }

    /** Record a new position if the player has moved enough since last tick. */
    public static void push(double x, double y, double z) {
        long now = System.currentTimeMillis();
        if (hasLast) {
            double dx = x - lastX, dy = y - lastY, dz = z - lastZ;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < 1e-5) {
                // Player essentially stationary — still push lightly so the trail
                // drifts rather than locking in one spot.
                if (!POINTS.isEmpty()) {
                    Point last = POINTS.peekLast();
                    if (now - last.timeMs < 80L) return;
                }
            }
        }
        POINTS.addLast(new Point(x, y, z, now));
        while (POINTS.size() > MAX_POINTS) POINTS.removeFirst();
        lastX = x; lastY = y; lastZ = z;
        hasLast = true;
    }

    /** Drop points older than {@code maxAgeMs}. */
    public static void ageOut(long maxAgeMs) {
        long now = System.currentTimeMillis();
        while (!POINTS.isEmpty() && now - POINTS.peekFirst().timeMs > maxAgeMs) {
            POINTS.removeFirst();
        }
    }

    private TrailHistory() {}
}
