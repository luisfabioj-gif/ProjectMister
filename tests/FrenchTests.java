package com.projectmister.game;

public final class FrenchTests {
    static void check(boolean b){if(!b)throw new AssertionError();}
    static PromotionCampaign reload(PromotionCampaign p){String s=p.snapshot();PromotionCampaign r=PromotionCampaign.restore(s);check(r.snapshot().equals(s));return r;}
    public static void main(String[] args) {
        for(int mask=0;mask<8;mask++) {
            PromotionCampaign p=new FrenchPromotion(new int[]{18,19,20,21,22},new int[]{15,16,17});
            int winner=-1;
            for(int round=0;round<3;round++) {
                KnockoutTie t=p.current();
                if(round==0)check(t.firstHome==21&&t.firstAway==22);
                if(round==1)check(t.firstHome==20&&t.firstAway==winner);
                if(round==2)check(t.firstHome==winner&&t.firstAway==15);
                check(t.legs==(round<2?1:2)&&!t.neutral);
                check(t.rule==(round<2?KnockoutTie.Rule.PENALTIES:KnockoutTie.Rule.EXTRA_TIME_PENALTIES));
                winner=(mask&(1<<round))==0?t.firstHome:t.firstAway;
                while(t.phase()==KnockoutTie.Phase.REGULATION){t.recordRegulation(t.home()==winner?2:0,t.away()==winner?2:0);p=reload(p);t=p.ties().get(round);}
                p=reload(p);
            }
            check(p.complete());check(p.promoted().length==(winner==15?2:3));
            check(p.promoted().length==p.relegated().length&&p.promoted()[0]==18&&p.promoted()[1]==19);
        }
        PromotionCampaign p=new FrenchPromotion(new int[]{18,19,20,21,22},new int[]{15,16,17});
        while(!p.complete()) {
            KnockoutTie t=p.current();int round=p.ties().size()-1;
            if(t.phase()==KnockoutTie.Phase.REGULATION)t.recordRegulation(0,0);
            else if(t.phase()==KnockoutTie.Phase.EXTRA_TIME){check(round==2);t.recordExtraTime(0,0);}
            else {check(t.aggregate(t.firstHome)==0);t.recordPenalties(5,4);}
            p=reload(p);
        }
        check(p.ties().get(0).extraTimeScore()[0]==-1&&p.ties().get(1).extraTimeScore()[0]==-1);
        try{PromotionCampaign.restore(p.snapshot().replace(";1;21;0;PENALTIES",";2;21;0;PENALTIES"));throw new AssertionError();}catch(IllegalArgumentException|IllegalStateException expected){}
        try{new FrenchPromotion(new int[]{0,1,2,3,4},new int[]{4,5,6});throw new AssertionError();}catch(IllegalArgumentException expected){}
        int[] pts={30,30,0},gf={40,35,0},ga={20,15,0},wins={8,9,0};Integer[] pair={0,1};
        LeagueResults history=new LeagueResults(3,true);history.record(0,0,1,0,2);
        FrenchStandings s=new FrenchStandings(pair,pts,gf,ga,wins,history,false);check(s.order[0]==0); // direct matches not complete: total GF
        history.record(1,1,0,1,0);
        s=new FrenchStandings(pair,pts,gf,ga,wins,history,true);check(s.order[0]==1); // direct points precede GF
        gf[0]=41;s=new FrenchStandings(pair,pts,gf,ga,wins,history,true);check(s.order[0]==0); // overall GD first
        gf[0]=40;s=new FrenchStandings(pair,pts,gf,ga,wins,new LeagueResults(3,false),true);check(s.tiedAt(0));
        gf[1]=40;ga[1]=20;history=new LeagueResults(3,true);history.record(0,0,1,1,1);history.record(1,1,0,0,0);
        s=new FrenchStandings(pair,pts,gf,ga,wins,history,true);check(s.order[0]==1); // wins
        wins[1]=8;history.record(2,2,1,0,1);s=new FrenchStandings(pair,pts,gf,ga,wins,history,true);check(s.order[0]==1); // away wins
        history=new LeagueResults(3,true);history.record(0,0,1,0,0);history.record(1,1,0,0,0);
        s=new FrenchStandings(pair,pts,gf,ga,wins,history,true);check(s.tiedAt(0)&&s.rankAt(1)==1); // unknown discipline cannot become an ID decision
        // Three-club circular results are resolved as a mini-table, not a pairwise comparator.
        history=new LeagueResults(3,true);int r=0;
        for(int a=0;a<3;a++)for(int b=0;b<3;b++)if(a!=b)history.record(r++,a,b,(a==0&&b==1)?3:1,0);
        s=new FrenchStandings(new Integer[]{2,1,0},new int[]{20,20,20},new int[]{20,20,20},new int[]{10,10,10},new int[]{5,5,5},history,true);
        check(s.order[0]==0&&s.order[2]==1);
        System.out.println("PASS: French eight playoff paths, no-extra-time eliminators, barrage retention, phase restores and national standings");
    }
}
