package com.projectmister.game;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/** Saved names/citizenships stay attached to player IDs through transfers and future data refreshes. */
public final class PlayerIdentities {
    public static String text(String s){return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    public static String untext(String s) {
        try {byte[] b=Base64.getUrlDecoder().decode(s);String v=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(b)).toString();if(!text(v).equals(s))throw new IllegalArgumentException("Noncanonical text");return v;}
        catch(Exception e){throw new IllegalArgumentException("Invalid identity text",e);}
    }
    public static String snapshot(List<String[]> rows) {
        StringBuilder b=new StringBuilder("PI1");for(String[] row:rows){validate(row);b.append('\n').append(text(row[0])).append('|').append(text(row[1]));}return b.toString();
    }
    public static List<String[]> restore(String raw,int expected) {
        if(expected<0||expected>CareerLimits.MAX_CLUBS*52+WorldMarket.MAX_HIRES||raw==null||raw.length()>4*1024*1024)throw new IllegalArgumentException("Invalid identity size");
        String[] lines=raw.split("\n",-1);if(!lines[0].equals("PI1")||lines.length!=expected+1)throw new IllegalArgumentException("Incomplete player identities");
        ArrayList<String[]> rows=new ArrayList<>();for(int i=1;i<lines.length;i++){String[] f=lines[i].split("\\|",-1);if(f.length!=2)throw new IllegalArgumentException("Invalid identity row");String[] row={untext(f[0]),untext(f[1])};validate(row);rows.add(row);}return rows;
    }
    private static void validate(String[] row){if(row==null||row.length!=2||row[0].trim().isEmpty()||row[0].length()>100||row[1].isEmpty()||row[1].length()>70||row[0].chars().anyMatch(Character::isISOControl)||row[1].chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid player identity");}
    private PlayerIdentities(){}
}
