package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

public final class PortugueseLowerPromotionTests {
    private static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
    private static void rejects(Runnable action){try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Invalid lower promotion accepted");}
    private static List<LocalDate> dates(int year){return Arrays.asList(LocalDate.of(year,7,25),LocalDate.of(year,8,1),LocalDate.of(year,8,8),LocalDate.of(year,9,5),LocalDate.of(year,10,27),LocalDate.of(year+1,1,5),LocalDate.of(year+1,1,6),LocalDate.of(year+1,1,9));}
    public static void main(String[] args) {
        int[] professionals=new int[36];for(int i=0;i<36;i++)professionals[i]=i;boolean[] reserves=new boolean[103];Arrays.fill(reserves,33,36,true);
        for(int year=2026;year<2030;year++)for(boolean upperFirst:new boolean[]{false,true})for(boolean retained:new boolean[]{false,true}) {
            if(year==2026&&upperFirst)continue;final int nextYear=year+1;
            PortugueseLowerPromotion p=new PortugueseLowerPromotion(year,new int[]{100,101,102},new int[]{30,31,32},upperFirst);
            rejects(p::promoted);rejects(p::leagueCupPreliminary);
            p.current().recordRegulation(1,1);p=PortugueseLowerPromotion.restore(p.snapshot());check(p.current().playedLegs()==1,"first leg retained");
            p.current().recordRegulation(1,1);p=PortugueseLowerPromotion.restore(p.snapshot());check(p.current().phase()==KnockoutTie.Phase.EXTRA_TIME,"aggregate tie proceeds to extra time");
            p.current().recordExtraTime(0,0);p=PortugueseLowerPromotion.restore(p.snapshot());
            KnockoutTie tie=p.current();boolean homeWins=(tie.home()==30)==retained;tie.recordPenalties(homeWins?5:4,homeWins?4:5);p=PortugueseLowerPromotion.restore(p.snapshot());
            check(p.complete()&&p.secondPromoted()==101&&p.playoffWinner()==(retained?30:102),"real barrage result determines qualifiers");
            check(Arrays.equals(p.leagueCupPreliminary(),retained?new int[]{30,101}:new int[]{101,102}),"article 7 home club follows previous division and rank");
            int[] field=p.nextProfessionalField(professionals,reserves);Set<Integer> ids=new HashSet<>();for(int c:field)ids.add(c);
            check(field.length==33&&ids.contains(100)&&ids.contains(101)&&!ids.contains(31)&&!ids.contains(32)&&ids.contains(30)==retained&&ids.contains(102)!=retained,"balanced lower transition, no duplicated clubs");
            PortugueseLeagueCupSeason cup=p.nextLeagueCup(17,professionals,reserves,new int[]{0,1,2,3,4,5},dates(nextYear),true);
            check(cup.current().stage==0&&cup.current().home==p.leagueCupPreliminary()[0]&&cup.current().away==p.leagueCupPreliminary()[1],"playoff feeds the actual next cup fixture");
            PortugueseLeagueCupSeason even=p.nextLeagueCup(17,professionals,reserves,new int[]{0,1,2,3,4},dates(nextYear),true);check(even.current().league,"even field skips preliminary");
            final PortugueseLowerPromotion finished=p;rejects(()->finished.nextLeagueCup(1,professionals,reserves,new int[]{0,1,2,3,4,31},dates(nextYear),true));
            String save=p.snapshot();rejects(()->PortugueseLowerPromotion.restore(save.replace("EXTRA_TIME_PENALTIES","HIGHER_SEED")));
        }
        PortugueseLowerPromotion reserveRelegated=new PortugueseLowerPromotion(2026,new int[]{100,101,102},new int[]{30,31,33},false);
        reserveRelegated.current().recordRegulation(0,1);
        reserveRelegated.current().recordRegulation(1,0);
        check(reserveRelegated.nextProfessionalField(professionals,reserves).length==34,"relegated reserve replaced by eligible first team increases cup field");
        check(reserveRelegated.nextLeagueCup(17,professionals,reserves,new int[]{0,1,2,3,4,5},dates(2027),true).current().league,"reserve relegation changes preliminary parity");
        rejects(()->new PortugueseLowerPromotion(2026,new int[]{100,101,102},new int[]{30,31,32},true));
        rejects(()->new PortugueseLowerPromotion(2027,new int[]{100,101,30},new int[]{30,31,32},false));
        System.out.println("PASS: lower Portuguese barrage outcomes, saved legs/ET/penalties, balanced promotion and actual next League Cup preliminary qualifiers");
    }
}
