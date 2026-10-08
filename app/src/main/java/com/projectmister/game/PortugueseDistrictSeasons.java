package com.projectmister.game;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PortugueseDistrictSeasons {
    private final PortugueseDistrictSeason active;
    private final List<String> archives;
    public PortugueseDistrictSeasons(PortugueseDistrictSeason active){this(active,new ArrayList<>());}
    private PortugueseDistrictSeasons(PortugueseDistrictSeason active,List<String> history){this.active=active;archives=Collections.unmodifiableList(new ArrayList<>(history));}
    public PortugueseDistrictSeason active(){return active;}
    public List<String> archives(){return archives;}
    public PortugueseDistrictSeasons next(PortugueseDistrictSeason next){if(!active.complete()||next.season!=active.season+1)throw new IllegalArgumentException("Finish current projected district qualifying season");List<String> history=new ArrayList<>(archives);history.add(active.snapshot());if(history.size()>50)history.remove(0);return new PortugueseDistrictSeasons(next,history);}
    private static String encode(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    public String snapshot(){StringBuilder out=new StringBuilder("PDQH1\n").append(encode(active.snapshot()));for(String s:archives)out.append('\n').append(encode(s));return out.toString();}
    public static PortugueseDistrictSeasons restore(String text){if(text==null||text.length()>4*1024*1024)throw new IllegalArgumentException("Invalid projected district qualifying archive size");String[] rows=text.split("\n",-1);if(rows.length<2||rows.length>52||!rows[0].equals("PDQH1"))throw new IllegalArgumentException("Invalid projected district qualifying archives");PortugueseDistrictSeason active=PortugueseDistrictSeason.restore(new String(Base64.getDecoder().decode(rows[1]),StandardCharsets.UTF_8));List<String> history=new ArrayList<>();int previous=-1;for(int i=2;i<rows.length;i++){String s=new String(Base64.getDecoder().decode(rows[i]),StandardCharsets.UTF_8);PortugueseDistrictSeason cup=PortugueseDistrictSeason.restore(s);if(!cup.complete()||cup.season>=active.season||previous>=0&&cup.season!=previous+1||cup.worldSize!=active.worldSize)throw new IllegalArgumentException("Invalid projected district qualifying archive chronology");history.add(s);previous=cup.season;}PortugueseDistrictSeasons result=new PortugueseDistrictSeasons(active,history);if(!result.snapshot().equals(text))throw new IllegalArgumentException("Non-canonical projected district qualifying archive");return result;}
}
