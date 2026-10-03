package com.projectmister.game;

public final class ScotlandPromotionTests {
    private static void check(boolean b){if(!b)throw new AssertionError();}
    private static void rejects(Runnable r){try{r.run();}catch(RuntimeException expected){return;}throw new AssertionError("Invalid state accepted");}
    public static void main(String[] args) {
        for(int winnerMask=0;winnerMask<8;winnerMask++) {
            ScotlandPromotion p=new ScotlandPromotion(12,11,10,13,14,15);
            rejects(p::promoted);
            for(int round=0;round<3;round++) {
                KnockoutTie tie=p.current();int expectedHigher=round==0?14:round==1?13:10;
                check(tie.firstAway==expectedHigher&&tie.home()==tie.firstHome);
                tie.recordRegulation(0,0);p=ScotlandPromotion.restore(p.snapshot());tie=p.current();
                check(tie.home()==expectedHigher&&tie.playedLegs()==1);
                boolean higherWins=(winnerMask&(1<<round))!=0;tie.recordRegulation(higherWins?1:0,higherWins?0:1);
                p=ScotlandPromotion.restore(p.snapshot());
            }
            check(p.complete()&&p.current()==null&&p.ties().size()==3);
            check(p.promoted()[0]==12&&p.relegated()[0]==11&&p.promoted().length==p.relegated().length);
            check(p.promoted().length==((winnerMask&4)!=0?1:2));
            check(ScotlandPromotion.restore(p.snapshot()).complete());
        }
        ScotlandPromotion p=new ScotlandPromotion(12,11,10,13,14,15);
        String initial=p.snapshot();rejects(()->ScotlandPromotion.restore(initial.replace("#1;15;14","#1;15;13")));
        rejects(()->ScotlandPromotion.restore(initial+"#1;14;13;2;13;0;EXTRA_TIME_PENALTIES"));
        rejects(()->new ScotlandPromotion(1,2,3,4,5,5));
        System.out.println("PASS: Scottish promotion ladder, both retention outcomes, all winner paths and reload validation");
    }
}
