package com.projectmister.game;
public final class TurkeyPromotionTests {
    static void check(boolean b){if(!b)throw new AssertionError();}
    static void win(KnockoutTie t,int club){while(t.phase()==KnockoutTie.Phase.REGULATION)t.recordRegulation(t.home()==club?2:0,t.away()==club?2:0);}
    public static void main(String[] args){
        for(int mask=0;mask<16;mask++){
            TurkeyPromotion p=new TurkeyPromotion(new int[]{18,19,20,21,22,23,24},new int[]{15,16,17});
            for(int round=0;round<4;round++){
                KnockoutTie t=p.current();check(t!=null);
                if(round==0)check(t.home()==21&&t.away()==24&&t.legs==1&&!t.neutral);
                if(round==1)check(t.home()==22&&t.away()==23&&t.legs==1&&!t.neutral);
                if(round==2)check(t.legs==2&&t.firstHome>t.firstAway&&!t.neutral);
                if(round==3)check(t.legs==1&&t.neutral&&t.firstHome==20);
                int winner=(mask&(1<<round))==0?t.firstHome:t.firstAway;
                win(t,winner);p=TurkeyPromotion.restore(p.snapshot());
            }
            check(p.complete()&&p.promoted().length==3&&p.relegated().length==3);
            check(p.promoted()[0]==18&&p.promoted()[1]==19);
            check(PromotionCampaign.restore(p.snapshot()).country().equals("TR"));
        }
        TurkeyPromotion p=new TurkeyPromotion(new int[]{18,19,20,21,22,23,24},new int[]{15,16,17});
        p.current().recordRegulation(0,0);p.current().recordExtraTime(0,0);p.current().recordPenalties(4,5);
        check(TurkeyPromotion.restore(p.snapshot()).ties().get(0).winner()==24);
        try{TurkeyPromotion.restore(p.snapshot().replace(";0;EXTRA_TIME_PENALTIES",";1;EXTRA_TIME_PENALTIES"));throw new AssertionError();}catch(IllegalArgumentException expected){}
        // Persist every pending knockout phase, including the neutral final's shootout.
        p=new TurkeyPromotion(new int[]{18,19,20,21,22,23,24},new int[]{15,16,17});
        int matches=0;
        while(!p.complete()) {
            KnockoutTie t=p.current();
            if(t.phase()==KnockoutTie.Phase.REGULATION){t.recordRegulation(0,0);matches++;}
            else if(t.phase()==KnockoutTie.Phase.EXTRA_TIME)t.recordExtraTime(0,0);
            else t.recordPenalties(4,5);
            String saved=p.snapshot();p=TurkeyPromotion.restore(saved);check(p.snapshot().equals(saved));
        }
        check(matches==5&&p.ties().get(3).neutral);
        check(p.ties().get(3).aggregate(p.ties().get(3).winner())==0); // Shootout does not inflate goals.
        try{new TurkeyPromotion(new int[]{18,19,20,21,22,23,23},new int[]{15,16,17});throw new AssertionError();}catch(IllegalArgumentException expected){}
        try{TurkeyPromotion.restore(p.snapshot()+"#"+p.ties().get(3).snapshot());throw new AssertionError();}catch(IllegalArgumentException expected){}
        System.out.println("PASS: Turkish sixteen playoff paths, seeded venues, neutral final, penalties and saved rounds");
    }
}
