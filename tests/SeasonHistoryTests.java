package com.projectmister.game;

import java.util.*;

public final class SeasonHistoryTests {
    private static void check(boolean ok){if(!ok)throw new AssertionError();}
    private static SeasonHistory.Entry entry(int year,int next){return new SeasonHistory.Entry(year+"-08-09","PT:club:1","Vitória ⚽","Liga Portugal 2",2,next,34,20,8,6,60,30,68);}
    private static void rejects(Runnable action){try{action.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("Accepted invalid history");}
    public static void main(String[] args)throws Exception {
        SeasonHistory empty=SeasonHistory.restore("");SeasonHistory one=empty.append(entry(2026,1));
        check(empty.entries().isEmpty());check(one.entries().get(0).outcome().equals("Promoted"));
        SeasonHistory two=one.append(entry(2027,2));String text=two.snapshot();
        check(SeasonHistory.restore(text).snapshot().equals(text));check(SeasonHistory.restore(text).entries().get(0).clubName.equals("Vitória ⚽"));
        rejects(()->two.append(entry(2027,1)));rejects(()->two.append(entry(2025,1)));rejects(()->SeasonHistory.restore("bad"));
        rejects(()->SeasonHistory.restore(text.substring(0,text.length()-8)));
        byte[] bytes=Base64.getDecoder().decode(text);byte[] trailing=Arrays.copyOf(bytes,bytes.length+1);
        rejects(()->SeasonHistory.restore(Base64.getEncoder().encodeToString(trailing)));
        rejects(()->new SeasonHistory.Entry("2026-08-09","id","club","league",1,2,3,1,1,0,1,1,4));
        Map<String,Object> fields=new HashMap<>();fields.put("save_1_season_history",text);
        check(SaveBackup.decode(SaveBackup.encode(fields)).get("save_1_season_history").equals(text));
        SeasonHistory full=new SeasonHistory();for(int i=0;i<1000;i++)full=full.append(entry(2026+i,2));
        check(SeasonHistory.restore(full.snapshot()).entries().size()==1000);final SeasonHistory bounded=full;rejects(()->bounded.append(entry(3026,2)));
        System.out.println("PASS: season archives, Unicode, rollover uniqueness, corruption rejection, bounds and backup retention");
    }
}
