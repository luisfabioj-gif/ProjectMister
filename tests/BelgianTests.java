package com.projectmister.game;
public final class BelgianTests {
    static void check(boolean b){if(!b)throw new AssertionError();}
    static void win(KnockoutTie t,int club){while(t.phase()==KnockoutTie.Phase.REGULATION)t.recordRegulation(t.home()==club?2:0,t.away()==club?2:0);}
    public static void main(String[] args){
        for(int mask=0;mask<8;mask++){
            PromotionCampaign p=new BelgianPromotion(new int[]{18,19,20,21,22},new int[]{16,17});
            for(int round=0;round<3;round++){
                KnockoutTie t=p.current();check(t.firstAway==t.higherSeed&&t.legs==2&&!t.neutral);
                if(round==0)check(t.firstHome==22&&t.firstAway==19);
                if(round==1)check(t.firstHome==21&&t.firstAway==20);
                win(t,(mask&(1<<round))==0?t.firstHome:t.firstAway);p=PromotionCampaign.restore(p.snapshot());
            }
            check(p.complete()&&p.promoted().length==2&&p.promoted()[0]==18&&p.relegated().length==2);
        }
        PromotionCampaign p=new BelgianPromotion(new int[]{18,19,20,21,22},new int[]{16,17});
        while(!p.complete()) {
            KnockoutTie t=p.current();if(t.phase()==KnockoutTie.Phase.REGULATION)t.recordRegulation(0,0);
            else if(t.phase()==KnockoutTie.Phase.EXTRA_TIME)t.recordExtraTime(0,0);else t.recordPenalties(4,5);
            String saved=p.snapshot();p=PromotionCampaign.restore(saved);check(p.snapshot().equals(saved));
        }
        try{PromotionCampaign.restore(p.snapshot().replace("EXTRA_TIME_PENALTIES","EXTRA_TIME_HIGHER_SEED"));throw new AssertionError();}catch(IllegalArgumentException|IllegalStateException expected){}
        int[] points={30,30,20,10,9,8,7,6},gf={50,20,10,0,0,0,0,0},ga={10,10,10,0,0,0,0,0},wins={8,9,0,0,0,0,0,0};
        Integer[] clubs={0,1,2,3,4,5,6,7};LeagueResults ledger=new LeagueResults(8,true);
        BelgianStandings s=new BelgianStandings(clubs,points,gf,ga,wins,ledger);check(s.order[0]==1); // wins beat GD
        check(s.eligible(new boolean[]{false,true,false,false,false,false,false,false})[0]==0);
        points=new int[]{20,20,0,0,0,0,0};gf=new int[]{20,20,0,0,0,0,0};ga=new int[]{10,10,0,0,0,0,0};wins=new int[]{5,5,0,0,0,0,0};
        ledger.record(0,2,0,0,1);ledger.record(1,3,1,0,2);
        s=new BelgianStandings(new Integer[]{0,1},points,gf,ga,wins,ledger);check(s.order[0]==1); // away GD
        ledger.record(2,4,0,0,1);s=new BelgianStandings(new Integer[]{0,1},points,gf,ga,wins,ledger);check(s.order[0]==0); // away wins
        s=new BelgianStandings(new Integer[]{0,1},points,gf,ga,wins,new LeagueResults(7,false));check(s.tiedAt(0)&&s.rankAt(1)==1);
        check(ReserveEligibility.parentRelegationConflict(new String[]{"be:club-brugge","be:club-nxt"},new boolean[]{false,true},new int[]{0}));
        System.out.println("PASS: Belgian eight winner paths, two promotions, saved shootouts, wins/away ranking and U23 eligibility");
    }
}
