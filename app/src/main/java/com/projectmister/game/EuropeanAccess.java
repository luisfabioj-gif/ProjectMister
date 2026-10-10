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
            case "PT": return new Profile(country, new Berth[]{cl("League phase"),cl("League phase"),cl("Q3 · league path"),el("Q2"),co("Q2")},el("League phase"),false);
            case "NL": return new Profile(country, new Berth[]{cl("League phase"),cl("Q3 · league path"),el("Q2"),co("Q2 · domestic play-off")},el("League phase"),false);
            case "BE": case "TR": return new Profile(country, new Berth[]{cl("League phase"),cl("Q3 · league path"),el("Q2"),co("Q2")},el("Play-off"),false);
            case "SCO": return new Profile(country, new Berth[]{cl("Q2 · champions path"),co("Q2"),co("Q2")},el("Q1"),false);
            default:
                int rank=rank(country);if(rank<1||country.equals("RU"))throw new IllegalArgumentException("No admitted UEFA access profile: "+country);
                if(country.equals("LI"))return new Profile(country,new Berth[0],co("Q1"),false);
                ArrayList<Berth> places=new ArrayList<>();places.add(cl(rank<=10?"League phase":rank<=14?"Play-off · champions path":rank<=22?"Q2 · champions path":"Q1 · champions path"));
                if(rank<=15)places.add(cl(rank<=9?"Q3 · league path":"Q2 · league path"));
                if(rank<=12){places.add(el("Q2"));places.add(co("Q2"));}
                else if(rank<=15){places.add(co("Q2"));places.add(co("Q2"));}
                else if(rank<=29){places.add(co("Q2"));places.add(co("Q2"));}
                else if(rank<=33){places.add(co("Q2"));places.add(co("Q1"));}
                else if(rank<=50){places.add(co("Q1"));places.add(co("Q1"));}
                else places.add(co("Q1"));
                Berth cup=rank<=7?el("League phase"):rank<=12?el("Play-off"):rank<=15?el("Q3"):rank<=33?el("Q1"):co(rank<=38?"Q2":"Q1");
                return new Profile(country,places.toArray(new Berth[0]),cup,false);
        }
    }
    /** Provisional rank fixed for projected future career access; not a forecast of UEFA's next circular. */
    public static int rank(String country){String[] order={"ENG","IT","ES","DE","FR","PT","NL","BE","TR","CZ","GR","PL","DK","NO","CY","CH","AT","SCO","SE","HR","IL","HU","UA","RS","RO","SI","AZ","RU","SK","BG","IE","IS","AM","MD","FI","KV","KZ","BA","LV","FO","MT","LI","EE","AL","MK","LT","NI","GI","AD","BY","LU","ME","GE","WAL","SM"};for(int i=0;i<order.length;i++)if(order[i].equals(country))return i+1;return -1;}
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
