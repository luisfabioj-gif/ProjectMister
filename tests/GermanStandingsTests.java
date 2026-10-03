package com.projectmister.game;
public final class GermanStandingsTests {
    private static void check(boolean b){if(!b)throw new AssertionError();}
    private static GermanStandings table(LeagueResults r){return new GermanStandings(new Integer[]{0,1,2},new int[]{12,12,5},new int[]{10,10,3},new int[]{5,5,8},r);}
    public static void main(String[] args){
        LeagueResults r=new LeagueResults(3,true);r.record(0,0,1,0,3);
        GermanStandings s=table(r);check(s.tiedAt(0)&&s.rankAt(1)==1); // One meeting must not decide an interim tie.
        r.record(1,1,0,0,1);s=table(r);check(s.order[0]==1&&!s.tiedAt(0));
        r=new LeagueResults(3,true);r.record(0,0,1,1,2);r.record(1,1,0,0,1);s=table(r);check(s.order[0]==1); // Equal aggregate; two away goals beat one.
        r=new LeagueResults(3,true);r.record(0,0,1,1,1);r.record(1,1,0,1,1);r.record(2,2,0,0,3);r.record(3,2,1,0,1);
        s=table(r);check(s.order[0]==0&&!s.tiedAt(0)); // All-away goals are last regular criterion.
        r=new LeagueResults(3,true);r.record(0,0,1,1,1);r.record(1,1,0,1,1);s=table(r);check(s.tiedAt(0));
        s=table(new LeagueResults(3,false));check(s.tiedAt(0));
        System.out.println("PASS: German head-to-head completion, aggregate, away-goal tie-breaks and unresolved positions");
    }
}
