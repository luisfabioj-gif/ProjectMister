package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Annual League Cup replacement keeps finished editions and requires fresh, separately qualified entrants. */
public final class PortugueseLeagueCupSeasons {
    private final List<String> archives;
    private final PortugueseLeagueCupSeason active;
    public PortugueseLeagueCupSeasons(PortugueseLeagueCupSeason active){this(Collections.emptyList(),active);}
    private PortugueseLeagueCupSeasons(List<String> archives,PortugueseLeagueCupSeason active) {
        if(active==null||archives.size()>100)throw new IllegalArgumentException("Invalid League Cup seasons");
        this.active=active;this.archives=Collections.unmodifiableList(new ArrayList<>(archives));int previous=-1;
        for(String saved:archives) {
            PortugueseLeagueCupSeason edition=PortugueseLeagueCupSeason.restore(saved);
            if(!edition.complete()||edition.season>=active.season||previous>=0&&edition.season!=previous+1)throw new IllegalArgumentException("Invalid League Cup archive");previous=edition.season;
        }
        if(previous>=0&&active.season!=previous+1)throw new IllegalArgumentException("Missing League Cup edition");
    }
    public PortugueseLeagueCupSeason active(){return active;}
    public List<String> archives(){return archives;}
    public PortugueseLeagueCupSeasons next(PortugueseLeagueCupSeason next) {
        if(next==null||!active.complete()||next.season!=active.season+1)throw new IllegalStateException("Finish League Cup before rollover");
        if(next.snapshot().split("\n",-1).length!=5)throw new IllegalArgumentException("Next League Cup already played");
        List<String> history=new ArrayList<>(archives);history.add(active.snapshot());PortugueseLeagueCupSeasons result=new PortugueseLeagueCupSeasons(history,next);
        if(result.snapshot().length()>4*1024*1024)throw new IllegalArgumentException("League Cup archive full");return result;
    }
    private static String encode(String value){return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));}
    private static String decode(String value){return new String(Base64.getDecoder().decode(value),StandardCharsets.UTF_8);}
    public String snapshot(){StringBuilder out=new StringBuilder("PTLCS1");for(String archive:archives)out.append('\n').append(encode(archive));return out.append('\n').append(encode(active.snapshot())).toString();}
    public static PortugueseLeagueCupSeasons restore(String text) {
        if(text==null||text.length()>4*1024*1024)throw new IllegalArgumentException("Oversized League Cup history");
        String[] lines=text.split("\n",-1);if(lines.length<2||lines.length>102||!lines[0].equals("PTLCS1"))throw new IllegalArgumentException("Invalid League Cup history");
        List<String> history=new ArrayList<>();for(int i=1;i<lines.length-1;i++)history.add(decode(lines[i]));
        PortugueseLeagueCupSeasons seasons=new PortugueseLeagueCupSeasons(history,PortugueseLeagueCupSeason.restore(decode(lines[lines.length-1])));
        if(!text.equals(seasons.snapshot()))throw new IllegalArgumentException("Non-canonical League Cup history");return seasons;
    }
}
