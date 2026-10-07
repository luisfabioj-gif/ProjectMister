package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Atomic annual replacement with immutable completed tournaments; no erased winners or replayed years. */
public final class CupSeasons {
    private final List<String> archives;
    private final SeasonCup active;
    public CupSeasons(SeasonCup active){this(Collections.emptyList(),active);}
    private CupSeasons(List<String> archives,SeasonCup active) {
        if(active==null||archives.size()>100)throw new IllegalArgumentException("Invalid cup seasons");
        this.archives=Collections.unmodifiableList(new ArrayList<>(archives));this.active=active;
        int previous=-1;
        for(String text:archives){SeasonCup cup=SeasonCup.restore(text);
            if(!cup.complete()||!cup.competition.equals(active.competition)||previous>=0&&cup.season!=previous+1||cup.season>=active.season)throw new IllegalArgumentException("Invalid cup archive");previous=cup.season;}
        if(previous>=0&&active.season!=previous+1)throw new IllegalArgumentException("Missing cup season");
    }
    public SeasonCup active(){return active;}
    public List<String> archives(){return archives;}
    public CupSeasons next(SeasonCup next) {
        if(!active.complete()||next==null||!next.competition.equals(active.competition)||next.season!=active.season+1)throw new IllegalStateException("Finish current cup before annual rollover");
        for(int i=0;i<next.drawnCount();i++)if(next.at(i).playedLegs()!=0)throw new IllegalArgumentException("Next cup already played");
        ArrayList<String> history=new ArrayList<>(archives);history.add(active.snapshot());return new CupSeasons(history,next);
    }
    public String snapshot() {
        StringBuilder out=new StringBuilder("CS1");
        for(String archived:archives)out.append('\n').append(encode(archived));out.append('\n').append(encode(active.snapshot()));return out.toString();
    }
    private static String encode(String text){return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));}
    public static CupSeasons restore(String text) {
        if(text==null||text.length()>6*1024*1024)throw new IllegalArgumentException("Oversized cup history");
        String[] lines=text.split("\n",-1);if(lines.length<2||lines.length>102||!lines[0].equals("CS1"))throw new IllegalArgumentException("Invalid cup history");
        ArrayList<String> history=new ArrayList<>();for(int i=1;i<lines.length-1;i++)history.add(new String(Base64.getDecoder().decode(lines[i]),StandardCharsets.UTF_8));
        SeasonCup active=SeasonCup.restore(new String(Base64.getDecoder().decode(lines[lines.length-1]),StandardCharsets.UTF_8));
        CupSeasons seasons=new CupSeasons(history,active);if(!text.equals(seasons.snapshot()))throw new IllegalArgumentException("Non-canonical cup history");return seasons;
    }
}
