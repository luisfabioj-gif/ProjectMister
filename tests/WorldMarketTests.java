package com.projectmister.game;
import java.util.*;
public final class WorldMarketTests {
    static void check(boolean b){if(!b)throw new AssertionError();}
    static void reject(String v){try{WorldMarket.restore(v);}catch(IllegalArgumentException expected){return;}throw new AssertionError(v);}
    public static void main(String[] args)throws Exception {
        WorldMarket.Candidate a=new WorldMarket.Candidate("eng:arsenal~1~4"),b=new WorldMarket.Candidate(a.key);
        check(a.name.equals(b.name)&&a.overall==b.overall&&a.fee>0);
        for(int i=0;i<60;i++)check(new WorldMarket.Candidate("free~0~"+i).fee==0);
        List<String> keys=WorldMarket.restore("eng:arsenal~1~4,free~0~59");check(WorldMarket.snapshot(keys).equals("eng:arsenal~1~4,free~0~59"));
        reject("free~1~0");reject("free~0~60");reject("eng:arsenal~1~20");reject("free~0~0,free~0~0");reject("anything");
        Map<String,Object> data=new HashMap<>();data.put("save_0_market_hires",WorldMarket.snapshot(keys));check(SaveBackup.decode(SaveBackup.encode(data)).equals(data));
        System.out.println("PASS: worldwide recruit identities, free-agent fees, malformed/duplicate saves and backup fields");
    }
}
