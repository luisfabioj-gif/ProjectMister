package com.projectmister.game;
import java.time.LocalDate;
public final class GermanyPromotionTests {
    private static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        LocalDate d=LocalDate.of(2027,5,22);
        for(int order=0;order<3;order++)for(boolean ballot:new boolean[]{false,true})for(boolean lowerWins:new boolean[]{false,true}) {
            GermanyPromotion p=new GermanyPromotion(18,19,16,17,15,20,d.plusDays(order==1?1:0),d.plusDays(order==2?1:0),ballot);
            boolean upperSecond=order==1||(order==0&&ballot);check(p.current().home()==(upperSecond?20:15));
            p.current().recordRegulation(0,0);p=GermanyPromotion.restore(p.snapshot());check(p.current().playedLegs()==1);
            KnockoutTie t=p.current();t.recordRegulation(0,0);t.recordExtraTime(0,0);
            boolean homeWins=(t.home()==20)==lowerWins;t.recordPenalties(homeWins?5:4,homeWins?4:5);
            p=GermanyPromotion.restore(p.snapshot());check(p.complete());check(p.promoted().length==(lowerWins?3:2));
            check(p.relegated().length==p.promoted().length);check(p.automaticPromoted()[0]==18);
            check(PromotionCampaign.restore(p.snapshot()).country().equals("DE"));
        }
        check(PromotionCampaign.restore(new ScotlandPromotion(20,11,10,21,22,23).snapshot()).country().equals("SCO"));
        try{GermanyPromotion.restore("DE#1;18;19;16;17;15;20#1;15;22;2;15;0;EXTRA_TIME_PENALTIES");throw new AssertionError();}catch(IllegalArgumentException expected){}
        System.out.println("PASS: German rest-day venue rules, both draws, retention/promotion and cross-country save dispatch");
    }
}
