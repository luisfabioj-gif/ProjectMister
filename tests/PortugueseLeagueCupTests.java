package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

public final class PortugueseLeagueCupTests {
    private static final String[] IDS={"pt:sl-benfica","pt:sporting-cp","pt:fc-porto","pt:sc-braga","pt:fc-famalicao","pt:gil-vicente-fc","pt:maritimo-m","pt:academico-de-viseu"};
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void rejects(Runnable r){try{r.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Invalid cup accepted");}
    public static void main(String[] args)throws Exception {
        Set<Integer> winners=new HashSet<>();
        for(int mask=0;mask<128;mask++) {
            PortugueseLeagueCup cup=PortugueseLeagueCup.create(IDS);int games=0;LocalDate previous=LocalDate.of(2026,8,9);
            check(cup.at(4)==null&&cup.at(6)==null,"future opponents not invented");
            check(cup.at(0).home()==2&&cup.at(0).away()==7,"Porto hosts Academico");
            check(cup.at(1).home()==1&&cup.at(1).away()==6,"Sporting hosts Maritimo");
            check(cup.at(2).home()==0&&cup.at(2).away()==5,"Benfica hosts Gil Vicente");
            check(cup.at(3).home()==3&&cup.at(3).away()==4,"Braga hosts Famalicao");
            check(!cup.due(LocalDate.of(2026,10,26))&&cup.due(LocalDate.of(2026,10,27)),"cup gates on date");
            while(!cup.complete()) {
                LocalDate date=cup.nextDate();check(!date.isBefore(previous),"chronological fixtures");previous=date;
                int index=cup.currentIndex();KnockoutTie tie=cup.current();
                check(tie.neutral==(index>=4),"only Final Four neutral");
                tie.recordRegulation(2,2);check(tie.phase()==KnockoutTie.Phase.PENALTIES,"no extra time");
                cup=PortugueseLeagueCup.restore(cup.snapshot(),IDS);tie=cup.current();
                boolean home=(mask&(1<<games))==0;tie.recordPenalties(home?5:4,home?4:5);
                check(tie.aggregate(tie.home())==2&&tie.aggregate(tie.away())==2,"penalties excluded from match score");
                cup=PortugueseLeagueCup.restore(cup.snapshot(),IDS);cup.validateDate(date);games++;
                if(games==4){check(cup.at(4).home()==cup.at(0).winner()&&cup.at(4).away()==cup.at(2).winner(),"semi 1 bracket");check(cup.at(5).home()==cup.at(1).winner()&&cup.at(5).away()==cup.at(3).winner(),"semi 2 bracket");}
            }
            check(games==7&&cup.at(6).home()==cup.at(4).winner()&&cup.at(6).away()==cup.at(5).winner(),"seven matches and fixed final");
            winners.add(cup.winner());check(cup.nextEventDate(LocalDate.of(2027,1,10)).equals(LocalDate.of(2027,1,10)),"league resumes after final");
            Map<String,Object> save=new HashMap<>();save.put("save_0_league_cup",cup.snapshot());
            check(SaveBackup.decode(SaveBackup.encode(save)).equals(save),"cup backup round trip");
        }
        check(winners.size()==8,"all entrants can win");
        PortugueseLeagueCup fresh=PortugueseLeagueCup.create(IDS);
        rejects(()->PortugueseLeagueCup.create(new String[]{"pt:sl-benfica"}));
        rejects(()->PortugueseLeagueCup.restore(fresh.snapshot()+"\n6:1;0;1;1;0;1;PENALTIES|R,1,0",IDS));
        rejects(()->fresh.validateDate(LocalDate.of(2026,10,28)));
        fresh.current().recordRegulation(1,0);String valid=fresh.snapshot();
        rejects(()->PortugueseLeagueCup.restore(valid.replace("|R,1,0","|R,-1,0"),IDS));
        rejects(()->PortugueseLeagueCup.restore(valid.replace("PENALTIES","EXTRA_TIME_PENALTIES"),IDS));
        rejects(()->PortugueseLeagueCup.restore(valid+"\n"+valid.split("\n")[1],IDS));
        rejects(()->fresh.validateDate(LocalDate.of(2026,10,26)));
        System.out.println("Portuguese League Cup: all 128 winner paths, chronology, direct penalties, save/backup and corruption checks passed");
    }
}
