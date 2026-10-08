package com.projectmister.game;
import java.util.*;
import java.io.*;
public final class SaveBackupTests {
    static void check(boolean b){if(!b)throw new AssertionError();}
    interface Work {void run()throws IOException;}
    static void reject(Work work)throws IOException {try{work.run();throw new AssertionError("Accepted invalid backup");}catch(IOException expected){}}
    public static void main(String[] args)throws Exception {
        Map<String,Object> values=new HashMap<>();values.put("save_0_exists",true);values.put("save_0_club",7);
        values.put("save_0_manager_first","Luís ⚽");values.put("save_0_classic_notes","first\u001Esecond\u001Fthird");
        values.put("save_0_national_cups","CS1\nGerman cup current edition and history");
        values.put("editor_name_pt:sl-benfica","Benfica");values.put("editor_primary_pt:sl-benfica",0xffaa0033);values.put("audio_crowd",false);
        byte[] bytes=SaveBackup.encode(values);check(SaveBackup.decode(bytes).equals(values));
        check(Arrays.equals(bytes,SaveBackup.encode(new TreeMap<>(values))));
        check(Arrays.equals(bytes,SaveBackup.read(new ByteArrayInputStream(bytes))));
        byte[] corrupt=bytes.clone();corrupt[20]^=1;reject(()->SaveBackup.decode(corrupt));
        reject(()->SaveBackup.decode(Arrays.copyOf(bytes,bytes.length-1)));
        reject(()->SaveBackup.decode(Arrays.copyOf(bytes,bytes.length+1)));
        reject(()->SaveBackup.read(null));
        Map<String,Object> bad=new HashMap<>(values);bad.put("save_0_club","7");reject(()->SaveBackup.encode(bad));
        bad.put("save_0_club",7);bad.put("save_9_exists",true);reject(()->SaveBackup.encode(bad));
        bad.remove("save_9_exists");bad.put("save_0_unknown",true);reject(()->SaveBackup.encode(bad));
        reject(()->SaveBackup.read(new ByteArrayInputStream(new byte[SaveBackup.MAX_BYTES+1])));
        check(SaveBackup.decode(SaveBackup.encode(Collections.emptyMap())).isEmpty());
        System.out.println("PASS: backup types, Unicode, deterministic round trips, checksum, truncation, unknown fields and size limits");
    }
}
