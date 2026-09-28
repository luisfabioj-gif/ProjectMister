package com.projectmister.game;

import java.util.*;

/** A stoppage is a transaction: only the net XI change consumes substitutions. */
final class SubstitutionLedger {
    private final int limit;
    private final Set<Integer> departed = new HashSet<>();
    private final Set<Integer> baseline = new HashSet<>();
    private final Set<Integer> current = new HashSet<>();
    private int committed;
    SubstitutionLedger(int limit) { this.limit = limit; }
    void reset(Collection<Integer> xi) {
        departed.clear(); baseline.clear(); baseline.addAll(xi);
        current.clear(); current.addAll(xi); committed = 0;
    }
    int used() {
        int n = committed;
        for (int id : baseline) if (!current.contains(id)) n++;
        return n;
    }
    boolean canChange(int off, int on) {
        if (off == on || !current.contains(off) || current.contains(on) || departed.contains(on)) return false;
        int net = used() + (baseline.contains(off) ? 1 : 0) - (baseline.contains(on) ? 1 : 0);
        return net <= limit;
    }
    boolean change(int off, int on) {
        if (!canChange(off, on)) return false;
        current.remove(off); current.add(on); return true;
    }
    boolean isReturn(int on) { return baseline.contains(on) && !current.contains(on); }
    boolean hasDeparted(int id) { return departed.contains(id); }
    void commit() {
        int net = used();
        for (int id : baseline) if (!current.contains(id)) departed.add(id);
        committed = net; baseline.clear(); baseline.addAll(current);
    }
}
