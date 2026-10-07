package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Recurring European seasons. Entrants and the next draw must come from the access/qualification layer. */
public final class EuropeanSeasons {
    private final List<String> archives;
    private final EuropeanSeason active;
    public EuropeanSeasons(EuropeanSeason active){this(Collections.emptyList(),active);}
    private EuropeanSeasons(List<String> archives,EuropeanSeason active) {
        if(active==null||archives.size()>100)throw new IllegalArgumentException("Invalid European seasons");
        this.active=active;this.archives=Collections.unmodifiableList(new ArrayList<>(archives));int previous=-1;
        for(String text:archives) {
            EuropeanSeason old=EuropeanSeason.restore(text);
            if(!old.complete()||old.leaguePhase().competition!=active.leaguePhase().competition||previous>=0&&old.leaguePhase().season!=previous+1||old.leaguePhase().season>=active.leaguePhase().season)throw new IllegalArgumentException("Invalid European archive");
            previous=old.leaguePhase().season;
        }
        if(previous>=0&&active.leaguePhase().season!=previous+1)throw new IllegalArgumentException("Missing European season");
    }
    public EuropeanSeason active(){return active;}
    public List<String> archives(){return archives;}
    public EuropeanSeasons next(EuropeanSeason next) {
        if(next==null||!active.complete()||next.leaguePhase().competition!=active.leaguePhase().competition||next.leaguePhase().season!=active.leaguePhase().season+1)throw new IllegalStateException("Finish current European season before rollover");
        if(!next.fresh())throw new IllegalArgumentException("Next European season already played");
        ArrayList<String> history=new ArrayList<>(archives);history.add(active.snapshot());return new EuropeanSeasons(history,next);
    }
    private static String encode(String text){return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));}
    private static String decode(String text){return new String(Base64.getDecoder().decode(text),StandardCharsets.UTF_8);}
    public String snapshot(){StringBuilder out=new StringBuilder("UES1");for(String text:archives)out.append('\n').append(encode(text));return out.append('\n').append(encode(active.snapshot())).toString();}
    public static EuropeanSeasons restore(String text) {
        if(text==null||text.length()>24*1024*1024)throw new IllegalArgumentException("Oversized European history");
        String[] rows=text.split("\n",-1);if(rows.length<2||rows.length>102||!rows[0].equals("UES1"))throw new IllegalArgumentException("Invalid European history");
        ArrayList<String> history=new ArrayList<>();for(int i=1;i<rows.length-1;i++)history.add(decode(rows[i]));
        EuropeanSeasons seasons=new EuropeanSeasons(history,EuropeanSeason.restore(decode(rows[rows.length-1])));
        if(!text.equals(seasons.snapshot()))throw new IllegalArgumentException("Non-canonical European history");return seasons;
    }
}
