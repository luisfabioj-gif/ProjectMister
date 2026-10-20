package com.projectmister.game;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Independent stand designs live in the career, across upgrades and backups. */
public final class StandDesigns {
    public static final int[] COLOURS={0xffc53228,0xff2d67c9,0xff2c975c,0xffefbe35,0xfff0f2ed,0xff222831,0xff883fc4,0xffed8734};
    public static final String[] COLOUR_NAMES={"Red","Blue","Green","Gold","White","Charcoal","Purple","Orange"};
    public static final String[] PATTERNS={"Solid seats","Club stripes","Checkerboard","Diagonal bands"};
    public static final class Design {
        public String name="",banner="",openDay="";
        public int colour=0,pattern=1;
    }
    private final Design[] stands={new Design(),new Design(),new Design(),new Design()};
    public Design get(int code){if(code<101||code>104)throw new IllegalArgumentException("Invalid stand");return stands[code-101];}
    public static String text(String s){return s==null?"":s.replaceAll("[\\p{Cntrl}]","").trim().substring(0,Math.min(36,s.replaceAll("[\\p{Cntrl}]","").trim().length()));}
    private static String pack(String s){return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    private static String unpack(String s){String v=new String(Base64.getUrlDecoder().decode(s),StandardCharsets.UTF_8);if(v.length()>36)throw new IllegalArgumentException("Stand text too long");return text(v);}
    public String snapshot(){StringJoiner out=new StringJoiner(";","1;","");for(Design d:stands)out.add(pack(text(d.name))+","+pack(text(d.banner))+","+d.colour+","+d.pattern+","+pack(d.openDay));return out.toString();}
    public static StandDesigns restore(String value){
        StandDesigns result=new StandDesigns();if(value==null||value.isEmpty())return result;
        if(value.length()>4096)throw new IllegalArgumentException("Oversized stand designs");
        String[] rows=value.split(";",-1);if(rows.length!=5||!rows[0].equals("1"))throw new IllegalArgumentException("Invalid stand designs");
        for(int i=0;i<4;i++){String[] p=rows[i+1].split(",",-1);if(p.length!=5)throw new IllegalArgumentException("Invalid stand design");Design d=result.stands[i];d.name=unpack(p[0]);d.banner=unpack(p[1]);d.colour=Integer.parseInt(p[2]);d.pattern=Integer.parseInt(p[3]);d.openDay=unpack(p[4]);if(d.colour<0||d.colour>=COLOURS.length||d.pattern<0||d.pattern>=PATTERNS.length||!d.openDay.isEmpty()&&!d.openDay.matches("\\d{4}-\\d{2}"))throw new IllegalArgumentException("Invalid stand options");}
        return result;
    }
}
