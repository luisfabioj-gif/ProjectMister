package com.projectmister.game;

import java.util.*;

public final class DutchTests {
    static void check(boolean value){if(!value)throw new AssertionError();}
    static DutchPromotion reload(DutchPromotion p){String s=p.snapshot();DutchPromotion r=(DutchPromotion)PromotionCampaign.restore(s);check(r.snapshot().equals(s));return r;}
    static DutchStandings table(){Integer[] ids=new Integer[20];int[] pts=new int[20],empty=new int[20];String[] names=new String[20];for(int i=0;i<20;i++){ids[i]=i;pts[i]=100-i;names[i]="Club "+i;}return new DutchStandings(ids,pts,empty,empty,empty,names,new LeagueResults(20,true),true);}
    static int[][][] periods(){int[][][] t=new int[4][20][3];for(int p=0;p<4;p++)for(int i=0;i<20;i++)t[p][i][0]=20-i;return t;}
    static boolean contains(int[] a,int id){for(int n:a)if(n==id)return true;return false;}
    public static void main(String[] args) {
        for(int mask=0;mask<64;mask++) {
            DutchPromotion p=new DutchPromotion(new int[]{20,21,22,23,24,25,26,27},new int[]{15,16,17});int[] winners=new int[6];int matches=0;
            for(int round=0;round<6;round++) {
                KnockoutTie t=p.current();check(t.legs==2&&!t.neutral&&t.firstAway==t.higherSeed);matches+=t.legs;
                if(round<3)check(t.firstHome==27-round&&t.firstAway==22+round);
                if(round==3)check(t.firstHome==Math.max(winners[0],winners[1])&&t.firstAway==Math.min(winners[0],winners[1]));
                if(round==4)check(t.firstHome==winners[2]&&t.firstAway==15);
                if(round==5)check(t.firstHome==Math.max(winners[3],winners[4])&&t.firstAway==Math.min(winners[3],winners[4]));
                winners[round]=(mask&(1<<round))==0?t.firstHome:t.firstAway;
                while(t.phase()==KnockoutTie.Phase.REGULATION){t.recordRegulation(t.home()==winners[round]?2:0,t.away()==winners[round]?2:0);p=reload(p);t=p.ties().get(round);}
            }
            check(matches==12&&p.complete()&&p.promoted().length==(winners[5]==15?2:3));check(p.promoted().length==p.relegated().length);
        }
        DutchPromotion p=new DutchPromotion(new int[]{20,21,22,23,24,25,26,27},new int[]{15,16,17});
        while(!p.complete()) {
            KnockoutTie t=p.current();
            if(t.phase()==KnockoutTie.Phase.REGULATION)t.recordRegulation(1,1);
            else if(t.phase()==KnockoutTie.Phase.EXTRA_TIME)t.recordExtraTime(0,0);
            else t.recordPenalties(5,4);
            p=reload(p);
        }
        try{DutchPromotion.restore(p.snapshot().replace(";EXTRA_TIME_PENALTIES",";HIGHER_SEED"));throw new AssertionError();}catch(IllegalArgumentException|IllegalStateException expected){}
        int[] counts=new int[4];for(int r=0;r<38;r++)counts[DutchQualification.periodOfRound(r)]++;
        check(Arrays.equals(counts,new int[]{10,9,9,10})&&DutchQualification.periodOfRound(19)==2);
        boolean[] reserves=new boolean[20];reserves[0]=true;
        int[][][] t=periods();int[] field=DutchQualification.select(table(),reserves,t);
        check(field[0]==1&&field[1]==2&&!contains(field,0)&&field.length==8); // repeated winner / reserve pass-down
        t=periods();for(int period=0;period<4;period++)t[period][10+period][0]=40;
        field=DutchQualification.select(table(),reserves,t);for(int id=10;id<14;id++)check(contains(field,id)); // low overall place can qualify
        t[0][19][0]=50;field=DutchQualification.select(table(),reserves,t);check(!contains(field,19)); // last-place exclusion
        t=periods();t[0][1][0]=t[0][0][0];field=DutchQualification.select(table(),reserves,t);check(field.length==8); // reserve/eligible ex aequo
        t=periods();t[0][2][0]=t[0][1][0]=30;
        try{DutchQualification.select(table(),reserves,t);throw new AssertionError();}catch(IllegalStateException expected){} // never invent discipline
        // Full result-derived periods, with strong scores so no disciplinary tie is needed.
        int[] ids=new int[20];for(int i=0;i<20;i++)ids[i]=i;
        LeagueSchedule schedule=new LeagueSchedule(ids,2);LeagueResults history=new LeagueResults(20,true);
        for(int r=0;r<38;r++)for(LeagueSchedule.Pairing game:schedule.round(r))history.record(r,game.home,game.away,game.home<game.away?60-game.home:0,game.away<game.home?60-game.away:0);
        field=DutchQualification.entrants(table(),reserves,history);check(field[0]==1&&field[1]==2&&field.length==8);
        try{DutchQualification.entrants(table(),reserves,new LeagueResults(20,true));throw new AssertionError();}catch(IllegalStateException expected){}
        int[] pts={30,30,0},played={10,11,0},gf={20,20,0},ga={10,10,0};String[] names={"Zulu","Alpha","Other"};Integer[] pair={0,1};
        history=new LeagueResults(3,true);history.record(0,0,1,1,1);history.record(1,1,0,2,2);
        DutchStandings s=new DutchStandings(pair,pts,played,gf,ga,names,history,false);check(s.order[0]==0); // fewer dropped points
        s=new DutchStandings(pair,pts,played,gf,ga,names,history,true);check(s.order[0]==0); // head-to-head away goals
        history=new LeagueResults(3,true);history.record(0,0,1,0,0);history.record(1,1,0,0,0);played[1]=10;
        s=new DutchStandings(pair,pts,played,gf,ga,names,history,false);check(s.order[0]==1&&!s.tiedAt(0)); // interim alphabetic
        s=new DutchStandings(pair,pts,played,gf,ga,names,history,true);check(s.tiedAt(0)&&s.rankAt(1)==1);
        check(ReserveEligibility.parentRelegationConflict(new String[]{"nl:ajax","nl:jong-ajax"},new boolean[]{false,true},new int[]{0}));
        System.out.println("PASS: Dutch 64 playoff paths, Eredivisie retention, phase restores, period boundaries/pass-down/reserves, result-led qualification and national ranking");
    }
}
