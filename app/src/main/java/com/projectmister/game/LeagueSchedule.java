package com.projectmister.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** One immutable fixture source for the calendar, watched match and background results. */
public final class LeagueSchedule {
    public static final class Pairing {
        public final int home, away;
        Pairing(int home, int away) { this.home = home; this.away = away; }
        public boolean contains(int club) { return home == club || away == club; }
        public int opponent(int club) {
            if (!contains(club)) throw new IllegalArgumentException("Club does not play this fixture");
            return home == club ? away : home;
        }
    }
    private final Pairing[][] rounds;

    /** Circle rotation supports odd leagues (bye omitted) and two, three or four meetings per pair. */
    public LeagueSchedule(int[] clubIds, int meetings) { this(clubIds, meetings, true); }

    /** Legacy venue order is retained only for development saves already using fixture version 1. */
    public LeagueSchedule(int[] clubIds, int meetings, boolean balancedVenues) {
        if (clubIds.length < 2 || (meetings != 2 && meetings != 3 && meetings != 4))
            throw new IllegalArgumentException("Invalid league format");
        HashSet<Integer> unique = new HashSet<>();
        for (int id : clubIds) if (id < 0 || !unique.add(id))
            throw new IllegalArgumentException("Invalid club IDs");
        int size = clubIds.length + clubIds.length % 2;
        int[] ring = new int[size];
        System.arraycopy(clubIds, 0, ring, 0, clubIds.length);
        if (size > clubIds.length) ring[size - 1] = -1;
        int cycle = size - 1;
        rounds = new Pairing[cycle * meetings][];
        for (int round = 0; round < cycle; round++) {
            List<Pairing> games = new ArrayList<>();
            for (int i = 0; i < size / 2; i++) {
                int a = ring[i], b = ring[size - 1 - i];
                if (a < 0 || b < 0) continue;
                // Only the fixed pairing alternates by round. Alternating every pairing by
                // round produces whole half-seasons away for some rotating clubs.
                boolean homeFirst = balancedVenues ? (i == 0 ? round % 2 == 0 : i % 2 == 0)
                        : (round + i) % 2 == 0;
                if (homeFirst) games.add(new Pairing(a, b));
                else games.add(new Pairing(b, a));
            }
            rounds[round] = games.toArray(new Pairing[0]);
            for (int leg = 1; leg < meetings; leg++) {
                Pairing[] reverse = new Pairing[games.size()];
                for (int i = 0; i < reverse.length; i++) {
                    Pairing p = games.get(i);
                    reverse[i] = leg % 2 == 1 ? new Pairing(p.away, p.home) : new Pairing(p.home, p.away);
                }
                rounds[round + cycle * leg] = reverse;
            }
            int tail = ring[size - 1];
            for (int i = size - 1; i > 1; i--) ring[i] = ring[i - 1];
            ring[1] = tail;
        }
    }
    public int roundCount() { return rounds.length; }
    public Pairing[] round(int index) {
        if (index < 0 || index >= rounds.length) throw new IllegalArgumentException("Round outside season");
        return rounds[index].clone();
    }
    /** Null means a genuine bye, never a fabricated opponent. */
    public Pairing fixture(int round, int club) {
        for (Pairing pairing : round(round)) if (pairing.contains(club)) return pairing;
        return null;
    }
}
