package com.projectmister.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Provisional 2027/28 base access, UEFA circular 54/2026 (9 September 2026).
 * Qualification is an outcome of a season; current participants are separate data.
 * Titleholder rebalancing and performance places must be supplied after they are known.
 */
public final class EuropeanAccess {
    public static final String SOURCE = "https://editorial.uefa.com/resources/02a9-218cd2086a08-882d0ce02937-1000/20260909_circular_2026_54_en.zip";
    public static final class Berth {
        public final String competition, stage;
        public Berth(String competition, String stage) { this.competition = competition; this.stage = stage; }
        public String label() { return competition + " · " + stage; }
    }
    public static final class Profile {
        public final String country;
        public final List<Berth> league;
        public final Berth cup;
        public final boolean conferenceFromLeagueCup;
        Profile(String country, Berth[] league, Berth cup, boolean conferenceFromLeagueCup) {
            this.country = country;
            List<Berth> copy = new ArrayList<>(); Collections.addAll(copy, league);
            this.league = Collections.unmodifiableList(copy); this.cup = cup;
            this.conferenceFromLeagueCup = conferenceFromLeagueCup;
        }
    }
    private static Berth cl(String stage) { return new Berth("Champions League", stage); }
    private static Berth el(String stage) { return new Berth("Europa League", stage); }
    private static Berth co(String stage) { return new Berth("Conference League", stage); }
    public static Profile profile(String country) {
        switch(country) {
            case "ENG": return new Profile(country, new Berth[]{cl("League phase"),cl("League phase"),cl("League phase"),cl("League phase"),el("League phase")},el("League phase"),true);
            case "ES": case "IT": case "DE":
                return new Profile(country, new Berth[]{cl("League phase"),cl("League phase"),cl("League phase"),cl("League phase"),el("League phase"),co("Play-off")},el("League phase"),false);
            case "FR": return new Profile(country, new Berth[]{cl("League phase"),cl("League phase"),cl("League phase"),cl("Q3 · league path"),el("League phase"),co("Play-off")},el("League phase"),false);
            case "PT": return new Profile(country, new Berth[]{cl("League phase"),cl("League phase"),cl("Q3 · league path"),el("Q2"),co("Q2")},el("Play-off"),false);
            case "NL": return new Profile(country, new Berth[]{cl("League phase"),cl("Q3 · league path"),el("Q2"),co("Q2 · domestic play-off")},el("League phase"),false);
            case "BE": case "TR": return new Profile(country, new Berth[]{cl("League phase"),cl("Q3 · league path"),el("Q2"),co("Q2")},el("Q3"),false);
            case "SCO": return new Profile(country, new Berth[]{cl("Q2 · champions path"),co("Q2"),co("Q2")},el("Q1"),false);
            default: throw new IllegalArgumentException("No verified UEFA access profile: " + country);
        }
    }
    /** Baseline projections only: a cup berth is not automatically awarded to a league position. */
    public static String positionLabel(String country, int position) {
        Profile p = profile(country);
        if (position < 1) throw new IllegalArgumentException("Position must be one-based");
        if (position <= p.league.size()) return p.league.get(position - 1).label() + "*";
        if (position <= p.league.size() + (p.conferenceFromLeagueCup ? 2 : 1)) return "Europe · cup-dependent*";
        return "";
    }
    private EuropeanAccess() {}
}
