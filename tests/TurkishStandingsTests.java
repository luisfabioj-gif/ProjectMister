package com.projectmister.game;
public final class TurkishStandingsTests {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        int[] pts={20,20,20},gf={30,20,10},ga={10,10,10};Integer[] clubs={0,1,2};
        LeagueResults r=new LeagueResults(3,true);
        r.record(0,0,1,0,1);r.record(1,1,0,1,0);r.record(2,0,2,2,0);r.record(3,2,0,0,2);r.record(4,1,2,0,1);r.record(5,2,1,1,0);
        TurkishStandings s=new TurkishStandings(clubs,pts,gf,ga,r,true);check(s.order[0]==0&&s.order[1]==1&&s.order[2]==2); // six points each; mini GD before season GD
        s=new TurkishStandings(new Integer[]{0,1},pts,gf,ga,r,true);check(s.order[0]==1); // direct points override overall GD
        s=new TurkishStandings(clubs,pts,gf,ga,new LeagueResults(3,false),true);check(s.tiedAt(0)&&s.rankAt(2)==1);
        s=new TurkishStandings(clubs,pts,gf,ga,new LeagueResults(3,true),false);check(s.order[0]==0&&!s.tiedAt(0));
        // All three have identical mini-table totals. Do not reapply the 1-over-0 direct result.
        r=new LeagueResults(3,true);
        r.record(0,0,1,0,1);r.record(1,1,0,1,0);r.record(2,0,2,1,0);r.record(3,2,0,0,1);r.record(4,1,2,0,1);r.record(5,2,1,1,0);
        s=new TurkishStandings(clubs,pts,gf,ga,r,true);check(s.order[0]==0&&s.order[1]==1&&s.order[2]==2);
        // No away-goal tie-break: equal direct aggregate goes to overall GD.
        r=new LeagueResults(3,true);r.record(0,0,1,2,1);r.record(1,1,0,1,0);
        s=new TurkishStandings(new Integer[]{0,1},pts,gf,ga,r,true);check(s.order[0]==0);
        s=new TurkishStandings(new Integer[]{0,1},pts,new int[]{20,20,0},new int[]{10,10,0},r,true);
        check(s.tiedAt(0)&&s.rankAt(1)==1);
        System.out.println("PASS: Turkish head-to-head priority, group mini-table, incomplete history and shared ranks");
    }
}
