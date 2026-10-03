package com.projectmister.game;

import java.util.ArrayList;
import java.util.List;

/** Original, deterministic knockout bookkeeping. Shootouts never inflate match goals. */
public final class KnockoutTie {
    public enum Rule { EXTRA_TIME_PENALTIES, EXTRA_TIME_HIGHER_SEED, HIGHER_SEED, PENALTIES }
    public enum Phase { REGULATION, EXTRA_TIME, PENALTIES, COMPLETE }
    public final int firstHome, firstAway, legs, higherSeed;
    public final boolean neutral;
    public final Rule rule;
    private final List<int[]> regulation=new ArrayList<>();
    private int extraHome=-1,extraAway=-1,penaltyHome=-1,penaltyAway=-1,winner=-1;
    private Phase phase=Phase.REGULATION;

    public KnockoutTie(int home,int away,int legs,int higherSeed,boolean neutral,Rule rule) {
        if(home<0||away<0||home==away||(legs!=1&&legs!=2)||(higherSeed!=home&&higherSeed!=away)
                ||rule==null||(neutral&&legs!=1))throw new IllegalArgumentException("Invalid knockout tie");
        firstHome=home;firstAway=away;this.legs=legs;this.higherSeed=higherSeed;this.neutral=neutral;this.rule=rule;
    }
    public Phase phase(){return phase;}
    public int playedLegs(){return regulation.size();}
    public int home(){return homeForLeg(Math.min(regulation.size(),legs-1));}
    public int away(){return home()==firstHome?firstAway:firstHome;}
    public int homeForLeg(int leg){if(leg<0||leg>=legs)throw new IllegalArgumentException("Invalid leg");return leg==0?firstHome:firstAway;}
    public int winner(){return winner;}
    public int loser(){return winner<0?-1:winner==firstHome?firstAway:firstHome;}
    public int[] regulationScore(int leg){return regulation.get(leg).clone();}
    public int[] extraTimeScore(){return new int[]{extraHome,extraAway};}
    public int[] penaltyScore(){return new int[]{penaltyHome,penaltyAway};}
    public int aggregate(int club) {
        if(club!=firstHome&&club!=firstAway)throw new IllegalArgumentException("Club outside tie");
        int total=0;for(int i=0;i<regulation.size();i++)total+=regulation.get(i)[homeForLeg(i)==club?0:1];
        if(extraHome>=0)total+=homeForLeg(legs-1)==club?extraHome:extraAway;
        return total;
    }
    private static void scores(int home,int away){if(home<0||away<0||home>99||away>99)throw new IllegalArgumentException("Invalid score");}
    private void complete(int club){winner=club;phase=Phase.COMPLETE;}
    private boolean resolveAggregate() {
        int h=aggregate(firstHome),a=aggregate(firstAway);
        if(h==a)return false;complete(h>a?firstHome:firstAway);return true;
    }
    public void recordRegulation(int homeGoals,int awayGoals) {
        if(phase!=Phase.REGULATION)throw new IllegalStateException("Regulation is not pending");
        scores(homeGoals,awayGoals);regulation.add(new int[]{homeGoals,awayGoals});
        if(regulation.size()<legs)return;
        if(resolveAggregate())return;
        if(rule==Rule.HIGHER_SEED)complete(higherSeed);
        else phase=rule==Rule.PENALTIES?Phase.PENALTIES:Phase.EXTRA_TIME;
    }
    public void recordExtraTime(int homeGoals,int awayGoals) {
        if(phase!=Phase.EXTRA_TIME)throw new IllegalStateException("Extra time is not pending");
        scores(homeGoals,awayGoals);extraHome=homeGoals;extraAway=awayGoals;
        if(resolveAggregate())return;
        if(rule==Rule.EXTRA_TIME_HIGHER_SEED)complete(higherSeed);else phase=Phase.PENALTIES;
    }
    public void recordPenalties(int homeGoals,int awayGoals) {
        if(phase!=Phase.PENALTIES)throw new IllegalStateException("Shootout is not pending");
        scores(homeGoals,awayGoals);if(homeGoals==awayGoals)throw new IllegalArgumentException("Shootout needs a winner");
        penaltyHome=homeGoals;penaltyAway=awayGoals;complete(homeGoals>awayGoals?home():away());
    }
    public String snapshot() {
        StringBuilder s=new StringBuilder("1;").append(firstHome).append(';').append(firstAway).append(';').append(legs)
                .append(';').append(higherSeed).append(';').append(neutral?1:0).append(';').append(rule.name());
        for(int[] r:regulation)s.append("|R,").append(r[0]).append(',').append(r[1]);
        if(extraHome>=0)s.append("|E,").append(extraHome).append(',').append(extraAway);
        if(penaltyHome>=0)s.append("|P,").append(penaltyHome).append(',').append(penaltyAway);
        return s.toString();
    }
    /** Replay validated events; never trust a saved winner, phase or aggregate. */
    public static KnockoutTie restore(String text) {
        if(text==null||text.length()>512)throw new IllegalArgumentException("Invalid knockout snapshot");
        String[] rows=text.split("\\|",-1),h=rows[0].split(";",-1);
        if(h.length!=7||!h[0].equals("1")||(!h[5].equals("0")&&!h[5].equals("1"))||rows.length>5)
            throw new IllegalArgumentException("Invalid knockout schema");
        KnockoutTie tie=new KnockoutTie(Integer.parseInt(h[1]),Integer.parseInt(h[2]),Integer.parseInt(h[3]),Integer.parseInt(h[4]),h[5].equals("1"),Rule.valueOf(h[6]));
        for(int i=1;i<rows.length;i++) {
            String[] e=rows[i].split(",",-1);if(e.length!=3)throw new IllegalArgumentException("Invalid knockout event");
            int home=Integer.parseInt(e[1]),away=Integer.parseInt(e[2]);
            switch(e[0]){case "R":tie.recordRegulation(home,away);break;case "E":tie.recordExtraTime(home,away);break;
                case "P":tie.recordPenalties(home,away);break;default:throw new IllegalArgumentException("Unknown knockout event");}
        }
        return tie;
    }
}
