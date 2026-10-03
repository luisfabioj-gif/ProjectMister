package com.projectmister.game;

public final class ScottishStandingsTests {
    private static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args) {
        Integer[] clubs={0,1,2};int[] groups={0,0,0},points={10,10,10},gf={9,9,9},ga={5,5,5};
        LeagueResults ledger=new LeagueResults(3,true);
        ledger.record(0,0,1,3,0);ledger.record(1,1,2,2,0);ledger.record(2,2,0,1,0);
        ScottishStandings s=new ScottishStandings(clubs,groups,points,gf,ga,ledger);
        check(s.order[0]==0&&s.order[1]==1&&s.order[2]==2&&!s.tiedAt(0)&&s.tiedAt(1));
        // Three-team circular wins are ranked using one shared mini-table, not pairwise comparison.
        s=new ScottishStandings(clubs,groups,points,gf,ga,new LeagueResults(3,false));check(s.tiedAt(0)&&s.tiedAt(1));
        groups[0]=1;s=new ScottishStandings(clubs,groups,points,gf,ga,ledger);check(s.order[2]==0);
        points[1]=11;s=new ScottishStandings(clubs,groups,points,gf,ga,ledger);check(s.order[0]==1);
        System.out.println("PASS: Scottish standings, circular head-to-head groups, incomplete history and locked split groups");
    }
}
