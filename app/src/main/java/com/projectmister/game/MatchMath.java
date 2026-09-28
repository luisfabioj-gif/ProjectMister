package com.projectmister.game;

/** Platform-independent match tuning, shared by both teams. */
final class MatchMath {
    static float clamp(float n, float lo, float hi) { return Math.max(lo, Math.min(hi, n)); }
    static float mobility(int pace, int fitness) {
        return (.68f + clamp(pace, 1, 100) * .006f) * (.60f + clamp(fitness, 0, 100) * .004f);
    }
    static float flightProgress(float time) {
        float t = clamp(time, 0, 1);
        return t * (1.22f - .22f * t); // fast release, modest drag; never accelerates from rest
    }
    static float arc(float time, float peak) {
        float t = clamp(time, 0, 1);
        return 4 * peak * t * (1-t);
    }
    static float intent(int goalsFor, int goalsAgainst, int minute) {
        int margin = goalsFor - goalsAgainst;
        if (minute < 60) return 0;
        if (margin < 0) return minute >= 78 ? 1f : .5f;
        if (margin > 0) return minute >= 78 ? -.7f : -.3f;
        return 0;
    }
}
