package com.projectmister.game;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PortugueseFourthDivisionSeasons {
    private final PortugueseFourthDivisionSeason active;
    private final List<String> archives;
    public PortugueseFourthDivisionSeasons(PortugueseFourthDivisionSeason active){this(active,new ArrayList<>());}
    private PortugueseFourthDivisionSeasons(PortugueseFourthDivisionSeason active,List<String> history){this.active=active;archives=Collections.unmodifiableList(new ArrayList<>(history));}
    public PortugueseFourthDivisionSeason active(){return active;}
    public List<String> archives(){return archives;}
    public PortugueseFourthDivisionSeasons next(PortugueseFourthDivisionSeason next){if(!active.complete()||next.season!=active.season+1)throw new IllegalArgumentException("Finish current Campeonato de Portugal season");List<String> history=new ArrayList<>(archives);history.add(active.snapshot());if(history.size()>50)history.remove(0);return new PortugueseFourthDivisionSeasons(next,history);}
    private static String encode(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    public String snapshot(){StringBuilder out=new StringBuilder("PCPH1\n").append(encode(active.snapshot()));for(String s:archives)out.append('\n').append(encode(s));return out.toString();}
    public static PortugueseFourthDivisionSeasons restore(String text){if(text==null||text.length()>4*1024*1024)throw new IllegalArgumentException("Invalid Campeonato de Portugal archive size");String[] rows=text.split("\n",-1);if(rows.length<2||rows.length>52||!rows[0].equals("PCPH1"))throw new IllegalArgumentException("Invalid Campeonato de Portugal archives");PortugueseFourthDivisionSeason active=PortugueseFourthDivisionSeason.restore(new String(Base64.getDecoder().decode(rows[1]),StandardCharsets.UTF_8));List<String> history=new ArrayList<>();int previous=-1;for(int i=2;i<rows.length;i++){String s=new String(Base64.getDecoder().decode(rows[i]),StandardCharsets.UTF_8);PortugueseFourthDivisionSeason cup=PortugueseFourthDivisionSeason.restore(s);if(!cup.complete()||cup.season>=active.season||previous>=0&&cup.season!=previous+1||cup.worldSize!=active.worldSize)throw new IllegalArgumentException("Invalid Campeonato de Portugal archive chronology");history.add(s);previous=cup.season;}PortugueseFourthDivisionSeasons result=new PortugueseFourthDivisionSeasons(active,history);if(!result.snapshot().equals(text))throw new IllegalArgumentException("Non-canonical Campeonato de Portugal archive");return result;}
}
