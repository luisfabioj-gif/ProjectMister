package com.projectmister.game;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class DomesticCupTests {
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static void rejects(Runnable r){try{r.run();}catch(IllegalArgumentException|IllegalStateException e){return;}throw new AssertionError("Invalid cup accepted");}
    private static int[] range(int a,int b){return java.util.stream.IntStream.range(a,b).toArray();}
    private static DomesticCup fresh(long seed){return new DomesticCup(149,seed,range(0,94),range(94,113),range(113,127),range(127,141),range(141,146));}
    public static void main(String[] args)throws Exception {
        int[] expected={47,40,27,16,8,4,2,1};
        for(int seed=0;seed<64;seed++) {
            DomesticCup cup=fresh(seed);Random random=new Random(seed);int[] counts=new int[8];int games=0;
            LocalDate previous=LocalDate.of(2026,8,9);Set<Integer> seen=new HashSet<>();
            check(cup.drawnCount()==47,"only opening draw initially known");
            boolean[] reserves=new boolean[149];Arrays.fill(reserves,146,149,true);cup.validateEntrants(reserves);
            while(!cup.complete()) {
                int index=cup.currentIndex(),stage=cup.stage(index);LocalDate date=cup.nextDate();
                check(!date.isBefore(previous),"chronological cup");previous=date;counts[stage]++;
                KnockoutTie tie=cup.current();seen.add(tie.home());seen.add(tie.away());
                check(tie.neutral==(stage>=6)&&tie.legs==1,"single ties and neutral final four");
                if(stage==1&&tie.away()>=113)check(tie.away()<127&&tie.home()<113,"second division enters away");
                if(stage==2&&tie.away()>=127)check(tie.away()<141&&tie.home()<127,"top division enters away");
                if(games%3==0) {
                    tie.recordRegulation(1,1);cup=DomesticCup.restore(cup.snapshot(),149);tie=cup.current();
                    check(tie.phase()==KnockoutTie.Phase.EXTRA_TIME,"extra time survives save");
                    tie.recordExtraTime(0,0);cup=DomesticCup.restore(cup.snapshot(),149);tie=cup.current();
                    check(tie.phase()==KnockoutTie.Phase.PENALTIES,"shootout survives save");
                    boolean home=random.nextBoolean();tie.recordPenalties(home?5:4,home?4:5);
                    check(tie.aggregate(tie.home())==1,"shootout excluded from score");
                } else tie.recordRegulation(random.nextBoolean()?2:0,1);
                cup=DomesticCup.restore(cup.snapshot(),149);cup.validateDate(date);games++;
            }
            check(games==145&&Arrays.equals(counts,expected)&&seen.size()==146,"complete field and bracket");
            check(cup.winner()>=0&&cup.winner()<146,"eligible champion");
            Map<String,Object> save=new HashMap<>();save.put("save_0_domestic_cup",cup.snapshot());save.put("save_0_season_age_applied",true);save.put("save_0_postseason_weeks",8);
            check(SaveBackup.decode(SaveBackup.encode(save)).equals(save),"cup and season state backup");
        }
        DomesticCup cup=fresh(7);rejects(()->cup.validateDate(LocalDate.of(2026,8,31)));
        cup.current().recordRegulation(1,0);String saved=cup.snapshot();
        rejects(()->cup.validateDate(LocalDate.of(2026,8,29)));
        rejects(()->DomesticCup.restore(saved,148));
        rejects(()->DomesticCup.restore(saved.replace("|R,1,0","|R,-1,0"),149));
        rejects(()->DomesticCup.restore(saved.replace("EXTRA_TIME_PENALTIES","PENALTIES"),149));
        rejects(()->DomesticCup.restore(saved+"\n"+saved.split("\n")[6],149));
        boolean[] reserves=new boolean[149];reserves[0]=true;rejects(()->cup.validateEntrants(reserves));
        String[] blocked={"2026-08-30","2026-09-20","2026-10-18","2026-10-27","2026-10-28","2026-10-29","2026-11-22","2026-12-16","2027-01-05","2027-01-06","2027-01-09","2027-02-03","2027-05-22","2027-05-23","2027-05-30"};
        List<LocalDate> dates=CompetitionCalendar.portugueseCupSeason(LocalDate.of(2026,8,9),34);
        for(int i=0;i<dates.size();i++) {
            if(i>0)check(ChronoUnit.DAYS.between(dates.get(i-1),dates.get(i))>=3,"league recovery gap");
            for(String date:blocked)check(Math.abs(ChronoUnit.DAYS.between(dates.get(i),LocalDate.parse(date)))>=3,"league avoids cup window");
        }
        rejects(()->CompetitionCalendar.portugueseCupSeason(LocalDate.of(2027,8,9),34));
        System.out.println("Domestic cup: 64 full tournaments, 146 entrants, staged draws, extra time, penalties, save integrity and calendar passed");
    }
}
