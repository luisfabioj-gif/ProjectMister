package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public final class ScottishLeagueCupSeasons {
    private final ScottishLeagueCupSeason active;
    private final List<String> archives;
    public ScottishLeagueCupSeasons(ScottishLeagueCupSeason cup){this(cup,Collections.emptyList());}
    private ScottishLeagueCupSeasons(ScottishLeagueCupSeason cup,List<String> history){if(cup==null||history.size()>100)throw new IllegalArgumentException("Invalid Scottish cup history");active=cup;archives=Collections.unmodifiableList(new ArrayList<>(history));}
    public ScottishLeagueCupSeason active(){return active;}
    public List<String> archives(){return archives;}
    public ScottishLeagueCupSeasons next(ScottishLeagueCupSeason cup) {
        if(!active.complete()||cup.season!=active.season+1||cup.groupFixture(0).playedLegs()>0)throw new IllegalStateException("Invalid Scottish cup rollover");
        ArrayList<String> editions=new ArrayList<>(archives);editions.add(active.snapshot());if(editions.size()>100)editions.remove(0);return new ScottishLeagueCupSeasons(cup,editions);
    }
    public String snapshot(){StringBuilder out=new StringBuilder("SLCS1\n").append(encode(active.snapshot()));for(String history:archives)out.append('\n').append(encode(history));return out.toString();}
    private static String encode(String text){return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));}
    private static String decode(String text){return new String(Base64.getDecoder().decode(text),StandardCharsets.UTF_8);}
    public static ScottishLeagueCupSeasons restore(String text) {
        if(text==null||text.length()>8*1024*1024)throw new IllegalArgumentException("Oversized Scottish cup history");String[] rows=text.split("\n",-1);
        if(rows.length<2||rows.length>102||!rows[0].equals("SLCS1"))throw new IllegalArgumentException("Invalid Scottish cup history");
        ScottishLeagueCupSeason active=ScottishLeagueCupSeason.restore(decode(rows[1]));ArrayList<String> history=new ArrayList<>();int previous=-1;
        for(int i=2;i<rows.length;i++){String raw=decode(rows[i]);ScottishLeagueCupSeason cup=ScottishLeagueCupSeason.restore(raw);if(!cup.complete()||cup.season>=active.season||previous>=0&&cup.season!=previous+1)throw new IllegalArgumentException("Invalid Scottish archived edition");history.add(raw);previous=cup.season;}
        if(previous>=0&&previous!=active.season-1)throw new IllegalArgumentException("Missing Scottish cup edition");
        ScottishLeagueCupSeasons seasons=new ScottishLeagueCupSeasons(active,history);if(!text.equals(seasons.snapshot()))throw new IllegalArgumentException("Non-canonical Scottish cup history");return seasons;
    }
    public static ScottishLeagueCupSeasons validate(String text,String[] identities,boolean[] reserves,int year,LocalDate date) {
        if(identities==null||reserves.length!=identities.length)throw new IllegalArgumentException("Missing Scottish cup world");
        ScottishLeagueCupSeasons seasons=restore(text);if(seasons.active.season!=year)throw new IllegalArgumentException("Wrong Scottish cup year");seasons.active.validateDate(date);
        validateClubs(seasons.active,identities,reserves);for(String history:seasons.archives)validateClubs(ScottishLeagueCupSeason.restore(history),identities,reserves);return seasons;
    }
    private static void validateClubs(ScottishLeagueCupSeason cup,String[] identities,boolean[] reserves){for(int[] field:new int[][]{cup.clubs(),cup.exemptions()})for(int club:field)if(club>=identities.length||reserves[club]||!identities[club].startsWith("sco:"))throw new IllegalArgumentException("Invalid Scottish cup club");}
}
