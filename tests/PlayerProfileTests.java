package com.projectmister.game;
import java.time.*;
import java.util.*;

public final class PlayerProfileTests {
    private static int checks;
    private static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;}
    private static void rejects(Runnable action,String text){try{action.run();throw new AssertionError(text);}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args)throws Exception {
        RosterExtensions extensions=RosterExtensions.legacy(Arrays.asList("free~0~1"));
        extensions.academy("pt:sl-benfica",LocalDate.of(2008,7,1));extensions.market("free~0~2");extensions.academy("pt:sl-benfica",LocalDate.of(2009,7,1));
        extensions.validate(Arrays.asList("free~0~1","free~0~2"),new String[]{"pt:sl-benfica","pt:fc-porto"});
        String saved=extensions.snapshot();RosterExtensions restored=RosterExtensions.restore(saved);
        check(saved.equals(restored.snapshot()),"Interleaved market/academy origins reload exactly");
        check(restored.origins().get(0).market.equals("free~0~1")&&restored.origins().get(1).academy()&&restored.origins().get(2).market.equals("free~0~2")&&restored.origins().get(3).cohort==1,"Append-only identities preserve both market hires and academy players");
        check(restored.goalkeepers(2).containsAll(Arrays.asList(0,1,20,21,40,41,43))&&!restored.goalkeepers(2).contains(42),"Goalkeeper identity follows interleaved immutable player origins");
        rejects(()->restored.validate(Arrays.asList("free~0~2","free~0~1"),new String[]{"pt:sl-benfica","pt:fc-porto"}),"Reordered market identities rejected");
        final String originSave=saved;rejects(()->RosterExtensions.restore(originSave.replace("G|pt:sl-benfica|2009-07-01|1","G|pt:sl-benfica|2009-07-01|0")),"Changed academy cohort rejected");
        rejects(()->restored.market("free~0~1"),"Duplicate market identity rejected");
        PlayerTrainingHistory history=new PlayerTrainingHistory();LocalDate birth=LocalDate.of(2008,7,1),start=birth.plusYears(15),day=LocalDate.of(2026,7,1);
        history.seed(0,birth,0,"PT",start,0);
        check(!history.clubTrained(0,0,day.minusDays(1))&&history.clubTrained(0,0,day),"Thirty-six calendar months have an exact boundary");
        check(history.associationTrained(0,"PT",day)&&!history.associationTrained(0,"ENG",day),"Training depends on club association, not nationality");
        check(history.listB(0,0,2026,LocalDate.of(2025,7,1)),"Two years since fifteen qualify a young player for List B");
        check(!history.listB(0,0,2026,LocalDate.of(2025,6,30)),"List B two-year boundary enforced");
        history.register(0,1,"PT",day);check(!history.clubTrained(0,1,day)&&history.associationTrained(0,"PT",day),"Domestic transfer retains association training but not new-club training");
        check(!history.listB(0,1,2026,day),"A young recent signing is not automatically List B");
        saved=history.snapshot();history=PlayerTrainingHistory.restore(saved);check(saved.equals(history.snapshot()),"Birth and registration transition reload exactly");history.validate(new int[]{1},day);
        PlayerTrainingHistory loan=new PlayerTrainingHistory();loan.seed(0,birth,0,"PT",start,0);loan.register(0,1,"PT",LocalDate.of(2024,7,1),0);loan.register(0,0,"PT",LocalDate.of(2025,7,1));check(loan.listB(0,0,2026,day),"Three years with one same-association loan of one year retain List B");
        PlayerTrainingHistory foreign=new PlayerTrainingHistory();foreign.seed(0,birth,0,"PT",start,0);foreign.register(0,1,"ENG",LocalDate.of(2024,7,1),0);foreign.register(0,0,"PT",LocalDate.of(2025,7,1));check(!foreign.listB(0,0,2026,day),"Foreign loan does not receive the domestic List B exception");
        PlayerTrainingHistory sixteen=new PlayerTrainingHistory();sixteen.seed(0,LocalDate.of(2010,7,1),0,"PT",LocalDate.of(2024,7,1),0);check(sixteen.listB(0,0,2026,day),"Sixteen-year-old with two uninterrupted years has the explicit exception");
        PlayerTrainingHistory older=new PlayerTrainingHistory();older.seed(0,LocalDate.of(2004,12,31),0,"PT",LocalDate.of(2019,12,31),0);check(!older.listB(0,0,2026,day),"List B birth cutoff is fixed to competition edition");
        PlayerTrainingHistory leap=new PlayerTrainingHistory();leap.seed(0,LocalDate.of(2005,1,1),0,"PT",LocalDate.of(2020,2,29),0);check(leap.clubTrained(0,0,LocalDate.of(2023,2,28))&&!leap.clubTrained(0,0,LocalDate.of(2023,2,27)),"Thirty-six months beginning on leap day have an exact calendar boundary");
        PlayerTrainingHistory whole=new PlayerTrainingHistory();whole.seed(0,LocalDate.of(2011,6,1),0,"PT",LocalDate.of(2026,7,1),0);LocalDate first=LocalDate.of(2026,8,9),last=LocalDate.of(2027,5,15);whole.completeSeason(0,first,last);String once=whole.snapshot();whole.completeSeason(0,first,last);check(once.equals(whole.snapshot()),"Repeated season completion cannot double-credit youth training");
        whole=PlayerTrainingHistory.restore(once);whole.completeSeason(0,first,last);check(once.equals(whole.snapshot()),"Season-credit idempotence survives reload");
        final PlayerTrainingHistory credited=whole;rejects(()->credited.completeSeason(0,first.plusDays(1),last),"Changing an already credited domestic calendar rejected");
        whole.completeSeason(0,LocalDate.of(2027,8,9),LocalDate.of(2028,5,15));whole.completeSeason(0,LocalDate.of(2028,8,9),LocalDate.of(2029,5,15));check(!whole.clubTrained(0,0,LocalDate.of(2029,5,14))&&whole.clubTrained(0,0,LocalDate.of(2029,5,15)),"Three completed seasons qualify only after the actual final match, never in a past-date preview");
        final PlayerTrainingHistory validated=history;rejects(()->validated.validate(new int[]{0},day),"Saved training club must agree with actual player team");rejects(()->validated.register(0,0,"PT",day.minusDays(1)),"Registration cannot move backwards");
        Map<String,Object> backup=new HashMap<>();for(int slot=0;slot<3;slot++){String prefix="save_"+slot+"_";backup.put(prefix+"player_profiles_version",1);backup.put(prefix+"roster_extensions",extensions.snapshot());backup.put(prefix+"player_training_history",history.snapshot());}
        check(backup.equals(SaveBackup.decode(SaveBackup.encode(backup))),"Every slot retains strictly typed player profiles through backups");
        System.out.println("PASS "+checks+" stable academy/market identities, calendar training, List B exceptions, saved births, season-credit idempotence and typed backups");
    }
}
