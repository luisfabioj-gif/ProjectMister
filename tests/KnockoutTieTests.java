package com.projectmister.game;

public final class KnockoutTieTests {
    private static void check(boolean b){if(!b)throw new AssertionError();}
    private static void rejects(Runnable r){try{r.run();}catch(RuntimeException expected){return;}throw new AssertionError("Invalid event accepted");}
    public static void main(String[] args) {
        KnockoutTie tie=new KnockoutTie(4,2,2,2,false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES);
        tie.recordRegulation(1,0);check(tie.phase()==KnockoutTie.Phase.REGULATION&&tie.home()==2&&tie.away()==4);
        tie=KnockoutTie.restore(tie.snapshot());tie.recordRegulation(2,1);
        // Away goals do not break a level aggregate.
        check(tie.phase()==KnockoutTie.Phase.EXTRA_TIME&&tie.aggregate(2)==2&&tie.aggregate(4)==2);
        tie=KnockoutTie.restore(tie.snapshot());tie.recordExtraTime(0,0);
        check(tie.phase()==KnockoutTie.Phase.PENALTIES);
        tie=KnockoutTie.restore(tie.snapshot());tie.recordPenalties(3,4);
        check(tie.winner()==4&&tie.loser()==2&&tie.aggregate(2)==2&&tie.aggregate(4)==2);
        check(KnockoutTie.restore(tie.snapshot()).winner()==4);
        final KnockoutTie finished=tie;rejects(()->finished.recordRegulation(0,0));rejects(()->finished.recordPenalties(4,3));
        KnockoutTie seeded=new KnockoutTie(8,3,2,3,false,KnockoutTie.Rule.EXTRA_TIME_HIGHER_SEED);
        seeded.recordRegulation(0,0);seeded.recordRegulation(1,1);seeded.recordExtraTime(0,0);
        check(seeded.winner()==3&&seeded.penaltyScore()[0]==-1);
        KnockoutTie single=new KnockoutTie(0,9,1,0,true,KnockoutTie.Rule.EXTRA_TIME_PENALTIES);
        rejects(()->single.recordExtraTime(1,0));single.recordRegulation(2,2);single.recordExtraTime(0,1);check(single.winner()==9);
        KnockoutTie direct=new KnockoutTie(2,5,1,2,false,KnockoutTie.Rule.PENALTIES);
        direct.recordRegulation(0,0);rejects(()->direct.recordPenalties(5,5));check(direct.phase()==KnockoutTie.Phase.PENALTIES);
        direct.recordPenalties(4,2);check(direct.winner()==2);
        KnockoutTie noExtra=new KnockoutTie(3,8,2,3,false,KnockoutTie.Rule.HIGHER_SEED);
        noExtra.recordRegulation(2,0);noExtra.recordRegulation(2,0);check(noExtra.winner()==3);
        for(KnockoutTie.Rule rule:KnockoutTie.Rule.values())for(int legs=1;legs<=2;legs++) {
            KnockoutTie clean=new KnockoutTie(41,2,legs,2,false,rule);clean.recordRegulation(4,0);
            if(legs==2)clean.recordRegulation(1,0);check(clean.winner()==41);
            String snapshot=clean.snapshot();check(KnockoutTie.restore(snapshot).snapshot().equals(snapshot));
        }
        rejects(()->new KnockoutTie(1,1,2,1,false,KnockoutTie.Rule.PENALTIES));
        rejects(()->new KnockoutTie(1,2,2,1,true,KnockoutTie.Rule.PENALTIES));
        for(String bad:new String[]{"", "2;1;2;1;1;0;PENALTIES", "1;1;2;1;1;2;PENALTIES", "1;1;2;1;1;0;PENALTIES|P,4,3",
                "1;1;2;1;1;0;PENALTIES|R,1,0|R,1,0", "1;1;2;1;1;0;PENALTIES|R,-1,0"})rejects(()->KnockoutTie.restore(bad));
        System.out.println("PASS: knockout legs, aggregate rules, extra time, shootouts, seeded ties and save replay");
    }
}
