package com.projectmister.game;
public final class LeagueResultsTests {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void reject(Runnable action){boolean failed=false;try{action.run();}catch(RuntimeException expected){failed=true;}check(failed,"invalid result accepted");}
    public static void main(String[] args) {
        LeagueResults r=new LeagueResults(6,true);
        check(r.record(0,0,1,2,0),"first result ignored");
        check(!r.record(0,0,1,2,0)&&r.size()==1,"duplicate delivery counted twice");
        reject(()->r.record(0,0,1,3,0));reject(()->r.record(0,2,0,0,0));
        r.record(1,1,2,1,0);r.record(2,2,0,3,1);r.record(3,0,4,9,0);
        int[][] mini=r.miniTable(new int[]{0,1,2});
        check(mini[0][0]==2&&mini[0][1]==3&&mini[0][2]==3&&mini[0][3]==3&&mini[0][4]==1,"head-to-head includes unrelated match");
        check(mini[1][1]==3&&mini[2][1]==3,"three-way tie points wrong");
        LeagueResults copy=LeagueResults.restore(r.snapshot(),6);
        check(copy.size()==4&&copy.recordedFromStart&&copy.fixture(2,2,0).awayGoals==1,"result snapshot changed");
        reject(()->LeagueResults.restore(r.snapshot(),5));reject(()->LeagueResults.restore(r.snapshot()+"|0,0,1,2,0",6));
        reject(()->LeagueResults.restore("2;6;1",6));reject(()->LeagueResults.restore("1;6;1|0,0,0,1,2",6));
        check(!LeagueResults.restore(new LeagueResults(6,false).snapshot(),6).recordedFromStart,"old missing history marked complete");
        LeagueResults expanded=new LeagueResults(768,true);expanded.record(0,710,767,2,1);
        check(LeagueResults.restore(expanded.snapshot(),768).fixture(0,710,767).homeGoals==2,"expanded world remaps high club IDs");
        reject(()->new LeagueResults(769,true));reject(()->expanded.record(1,768,710,1,0));
        for(int n:new int[]{10,12,15,18,20,22,24}) {
            int[] ids=new int[n];for(int i=0;i<n;i++)ids[i]=i;
            LeagueSchedule schedule=new LeagueSchedule(ids,2);LeagueResults season=new LeagueResults(n,true);
            for(int round=0;round<schedule.roundCount();round++)for(LeagueSchedule.Pairing f:schedule.round(round))season.record(round,f.home,f.away,1,0);
            LeagueResults restored=LeagueResults.restore(season.snapshot(),n);
            check(restored.size()==n*(n-1),"full season loses results");
            for(int[] row:restored.miniTable(ids))check(row[0]==2*(n-1)&&row[1]==3*(n-1),"full season totals wrong");
        }
        System.out.println("PASS: full-season result history, idempotence, conflict rejection, three-team mini tables and save validation");
    }
}
