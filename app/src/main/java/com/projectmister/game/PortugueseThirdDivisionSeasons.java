package com.projectmister.game;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PortugueseThirdDivisionSeasons {
    private final PortugueseThirdDivisionSeason active;
    private final List<String> archives;
    public PortugueseThirdDivisionSeasons(PortugueseThirdDivisionSeason active){this(active,new ArrayList<>());}
    private PortugueseThirdDivisionSeasons(PortugueseThirdDivisionSeason active,List<String> history){this.active=active;archives=Collections.unmodifiableList(new ArrayList<>(history));}
    public PortugueseThirdDivisionSeason active(){return active;}
    public List<String> archives(){return archives;}
    public PortugueseThirdDivisionSeasons next(PortugueseThirdDivisionSeason next){if(!active.complete()||next.season!=active.season+1)throw new IllegalArgumentException("Finish current Liga 3 season");List<String> history=new ArrayList<>(archives);history.add(active.snapshot());if(history.size()>50)history.remove(0);return new PortugueseThirdDivisionSeasons(next,history);}
    private static String encode(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    public String snapshot(){StringBuilder out=new StringBuilder("PL3H1\n").append(encode(active.snapshot()));for(String s:archives)out.append('\n').append(encode(s));return out.toString();}
    public static PortugueseThirdDivisionSeasons restore(String text){if(text==null||text.length()>4*1024*1024)throw new IllegalArgumentException("Invalid Liga 3 archive size");String[] rows=text.split("\n",-1);if(rows.length<2||rows.length>52||!rows[0].equals("PL3H1"))throw new IllegalArgumentException("Invalid Liga 3 archives");PortugueseThirdDivisionSeason active=PortugueseThirdDivisionSeason.restore(new String(Base64.getDecoder().decode(rows[1]),StandardCharsets.UTF_8));List<String> history=new ArrayList<>();int previous=-1;for(int i=2;i<rows.length;i++){String s=new String(Base64.getDecoder().decode(rows[i]),StandardCharsets.UTF_8);PortugueseThirdDivisionSeason cup=PortugueseThirdDivisionSeason.restore(s);if(!cup.complete()||cup.season>=active.season||previous>=0&&cup.season!=previous+1||cup.worldSize!=active.worldSize)throw new IllegalArgumentException("Invalid Liga 3 archive chronology");history.add(s);previous=cup.season;}PortugueseThirdDivisionSeasons result=new PortugueseThirdDivisionSeasons(active,history);if(!result.snapshot().equals(text))throw new IllegalArgumentException("Non-canonical Liga 3 archive");return result;}
}
