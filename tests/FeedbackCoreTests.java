package com.projectmister.game;
import java.time.LocalDate;
import java.util.*;
public final class FeedbackCoreTests {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void rejects(Runnable r,String why){try{r.run();throw new AssertionError(why);}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args){
        int[] numbers={1,2,3,4,10,11,12,13,20,21,22,23,31,101,111,112,113};String[] labels={"1st","2nd","3rd","4th","10th","11th","12th","13th","20th","21st","22nd","23rd","31st","101st","111th","112th","113th"};
        for(int i=0;i<numbers.length;i++)check(DisplayFormat.ordinal(numbers[i]).equals(labels[i]),"Human ordinal "+numbers[i]);check(DisplayFormat.ordinal(0).equals("—"),"Unranked is not 0th");
        for(String nation:Arrays.asList("Portugal","England","Scotland","France","Germany","Italy","Spain","Netherlands","Turkey","Brazil","Japan","Nigeria","Morocco","Romania")){
            check(PlayerNames.name(nation,42).equals(PlayerNames.name(nation,42)),"Stable cultural identity");check(PlayerNames.name(nation,42).contains(" "),"Complete fictional name");
        }
        check(!PlayerNames.name("Japan",42).equals(PlayerNames.name("Portugal",42)),"Names actually change by nationality");
        check(PlayerNames.home("SCO").equals("Scotland")&&PlayerNames.home("DE").equals("Germany"),"Club association mapping");
        List<String[]> identities=Arrays.asList(new String[]{"João Gonçalves","Portugal"},new String[]{"Luka Kovačević","Croatia"},new String[]{"Yuto Sato","Japan"});String idSave=PlayerIdentities.snapshot(identities);
        List<String[]> restored=PlayerIdentities.restore(idSave,3);check(restored.get(0)[0].equals("João Gonçalves")&&restored.get(2)[1].equals("Japan"),"Unicode identities survive reload");
        check(PlayerIdentities.snapshot(restored).equals(idSave),"Exact identity backup");rejects(()->PlayerIdentities.restore(idSave,2),"Missing player identity rejected");rejects(()->PlayerIdentities.restore(idSave+"\n",3),"Trailing data rejected");rejects(()->PlayerIdentities.untext("_w"),"Malformed UTF-8 rejected");
        ScoutingReports book=new ScoutingReports();LocalDate day=LocalDate.of(2026,8,10);check(book.assign("P1",day),"Explicit assignment starts with masking independent state");check(!book.assign("P1",day),"Duplicate assignment does not restart the timer");
        check(book.due(day.plusDays(6)).isEmpty(),"No premature report");check(book.due(day.plusDays(7)).size()==1,"Report arrives after seven days");check(book.get("P1").report().isEmpty(),"No completed assessment invented while scouting");
        String pending=book.snapshot();book=ScoutingReports.restore(pending,3);check(book.snapshot().equals(pending),"Pending assignment survives exact reload");
        List<String[]> report=Arrays.asList(new String[]{"Player","Yuto Sato"},new String[]{"Current ability","Strong first-team player"},new String[]{"Potential","Could become a leading top-flight player"},new String[]{"Wage estimate","€24k per week"});
        final ScoutingReports pendingBook=book;rejects(()->pendingBook.record("P1",day.plusDays(6),report),"Early completion rejected");
        book.record("P1",day.plusDays(7),report);check(!book.get("P1").pending()&&book.get("P1").reported.equals(day.plusDays(7)),"Completed dated assessment");
        book.get("P1").report().get(0)[1]="Changed";check(book.get("P1").title().equals("Yuto Sato"),"Reading a report cannot mutate it");
        String complete=book.snapshot();check(ScoutingReports.restore(complete,3).snapshot().equals(complete),"Completed report survives exact reload");rejects(()->ScoutingReports.restore(complete,1),"Unknown player in backup rejected");rejects(()->ScoutingReports.restore(complete+"\n",3),"Corrupt report backup rejected");
        check(book.assign("P1",day.plusDays(20)),"Player can be scouted again");check(book.get("P1").title().equals("Yuto Sato")&&book.get("P1").pending(),"Old report retained during update");
        check(book.assign("Mfree~0~1",day),"Free agents can be scouted");check(ScoutingReports.restore(book.snapshot(),3).snapshot().equals(book.snapshot()),"Mixed market/player assignments preserve identities");
        System.out.println("PASS "+checks+" feedback-core checks: ordinals, cultural identities, timed scouting, saved reports and corrupt-backup rejection");
    }
}
