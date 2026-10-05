package com.projectmister.game;
public final class IberianTests {
    static void check(boolean b){if(!b)throw new AssertionError();}
    static void reject(Runnable action){try{action.run();throw new AssertionError("Accepted invalid data");}catch(IllegalArgumentException|IllegalStateException expected){}}
    static void win(KnockoutTie t,int club){while(t.phase()==KnockoutTie.Phase.REGULATION)t.recordRegulation(t.home()==club?2:0,t.away()==club?2:0);}
    public static void main(String[] args){
        for(boolean upperFirst:new boolean[]{false,true})for(boolean retained:new boolean[]{false,true}){
            PromotionCampaign p=new IberianPromotion("PT",new int[]{18,19,20},new int[]{15,16,17},upperFirst);
            check(p.current().home()==(upperFirst?15:20));
            int winner=retained?15:20;
            p.current().recordRegulation(0,0);p=PromotionCampaign.restore(p.snapshot());
            check(p.current().home()==(upperFirst?20:15));
            p.current().recordRegulation(0,0);p=PromotionCampaign.restore(p.snapshot());
            check(p.current().phase()==KnockoutTie.Phase.EXTRA_TIME);
            p.current().recordExtraTime(0,0);p=PromotionCampaign.restore(p.snapshot());
            p.current().recordPenalties(p.current().home()==winner?5:4,p.current().away()==winner?5:4);
            p=PromotionCampaign.restore(p.snapshot());check(p.complete());
            check(p.promoted().length==(retained?2:3)&&p.relegated().length==(retained?2:3));
            check(p.ties().get(0).aggregate(winner)==0);
        }
        for(int mask=0;mask<8;mask++){
            PromotionCampaign p=new IberianPromotion("ES",new int[]{20,21,22,23,24,25},new int[]{17,18,19},false);
            for(int round=0;round<3;round++){
                KnockoutTie t=p.current();check(t.firstAway==t.higherSeed&&t.legs==2&&!t.neutral);
                if(round==0)check(t.firstHome==25&&t.firstAway==22);
                if(round==1)check(t.firstHome==24&&t.firstAway==23);
                win(t,(mask&(1<<round))==0?t.firstHome:t.firstAway);
                p=PromotionCampaign.restore(p.snapshot());
            }
            check(p.complete()&&p.promoted().length==3&&p.relegated().length==3);
        }
        PromotionCampaign p=new IberianPromotion("ES",new int[]{20,21,22,23,24,25},new int[]{17,18,19},false);
        while(!p.complete()){
            KnockoutTie t=p.current();if(t.phase()==KnockoutTie.Phase.REGULATION)t.recordRegulation(1,1);
            else{check(t.phase()==KnockoutTie.Phase.EXTRA_TIME);t.recordExtraTime(0,0);check(t.winner()==t.higherSeed);}
            String saved=p.snapshot();p=PromotionCampaign.restore(saved);check(p.snapshot().equals(saved));
        }
        check(p.promoted()[2]==22);
        final String saved=p.snapshot();
        reject(()->PromotionCampaign.restore(saved.replace("EXTRA_TIME_HIGHER_SEED","EXTRA_TIME_PENALTIES")));
        reject(()->PromotionCampaign.restore(saved+"#"+saved.split("#")[2]));
        reject(()->new IberianPromotion("PT",new int[]{1,2,3},new int[]{3,4,5},false));
        LeagueResults ledger=new LeagueResults(3,true);
        ledger.record(0,0,1,0,1);
        int[] pts={10,10,10},gf={20,10,5},ga={0,0,0},wins={3,2,1};
        IberianStandings s=new IberianStandings("PT",new Integer[]{0,1},pts,gf,ga,wins,ledger,false);
        check(s.order[0]==1); // Portuguese interim direct points are already used.
        s=new IberianStandings("ES",new Integer[]{0,1},pts,gf,ga,wins,ledger,false);check(s.order[0]==0);
        s=new IberianStandings("PT",new Integer[]{0,1},pts,gf,ga,wins,ledger,true);check(s.tiedAt(0));
        ledger.record(1,1,0,0,1); // Direct points and GD tied.
        s=new IberianStandings("PT",new Integer[]{0,1},pts,new int[]{20,30,0},new int[]{10,20,0},wins,ledger,true);check(s.order[0]==0); // Wins before GF.
        s=new IberianStandings("ES",new Integer[]{0,1},pts,new int[]{20,30,0},new int[]{10,20,0},wins,ledger,true);check(s.order[0]==1);
        IberianStandings shared=new IberianStandings("PT",new Integer[]{0,1,2},pts,gf,ga,wins,new LeagueResults(3,false),true);
        reject(()->shared.eligible(new boolean[]{false,true,false},1));
        s=new IberianStandings("ES",new Integer[]{0,1,2},new int[]{30,20,10},gf,ga,wins,ledger,true);
        check(s.eligible(new boolean[]{true,false,false},2)[0]==1);
        check(ReserveEligibility.parentRelegationConflict(new String[]{"pt:fc-porto","pt:fc-porto-b"},new boolean[]{false,true},new int[]{0}));
        check(!ReserveEligibility.parentRelegationConflict(new String[]{"pt:fc-porto","pt:fc-porto-b"},new boolean[]{false,true},new int[]{1}));
        System.out.println("PASS: Portuguese draw/retention, Spanish eight playoff paths and higher-seed extra time, saved phases, ranking and reserve eligibility");
    }
}
