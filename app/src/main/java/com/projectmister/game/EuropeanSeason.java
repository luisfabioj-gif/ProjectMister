package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/** A league phase and its derived knockout bracket, with one authoritative saved state. */
public final class EuropeanSeason {
    private final EuropeanLeaguePhase phase;
    private EuropeanKnockout knockout;
    private final long seed;
    private final List<LocalDate> dates;
    public final boolean simulatedDates;
    public EuropeanSeason(EuropeanLeaguePhase phase,long seed,List<LocalDate> dates,boolean simulatedDates) {
        if(phase==null||dates==null||dates.size()!=9)throw new IllegalArgumentException("Invalid European season");
        LocalDate previous=phase.date(phase.competition==EuropeanLeaguePhase.Competition.CONFERENCE?5:7);
        for(LocalDate d:dates){if(d==null||!d.isAfter(previous)||d.isAfter(LocalDate.of(phase.season+1,6,30)))throw new IllegalArgumentException("Invalid European season dates");previous=d;}
        this.phase=phase;this.seed=seed;this.dates=Collections.unmodifiableList(new ArrayList<>(dates));this.simulatedDates=simulatedDates;
    }
    public EuropeanLeaguePhase leaguePhase(){return phase;}
    public EuropeanKnockout knockout(){if(knockout==null&&phase.complete())knockout=new EuropeanKnockout(phase,seed,dates,simulatedDates);return knockout;}
    public boolean complete(){EuropeanKnockout cup=knockout();return cup!=null&&cup.complete();}
    public int winner(){return complete()?knockout.winner():-1;}
    public LocalDate nextDate(){if(!phase.complete())return phase.date(phase.currentRound());return knockout().nextDate();}
    public boolean fresh(){for(int i=0;i<phase.fixtureCount();i++)if(phase.result(i)!=null)return false;return true;}
    public void validateDate(LocalDate date){phase.validateDate(date);if(phase.complete())knockout().validateDate(date);}
    public String snapshot() {
        // An unresolved final ranking is preserved as league state until a sporting decision is supplied.
        StringBuilder out=new StringBuilder("US1|").append(seed).append('|').append(simulatedDates?1:0);
        for(LocalDate d:dates)out.append('|').append(d);
        out.append('\n').append(knockout==null?"L|":"K|").append(encode(knockout==null?phase.snapshot():knockout.snapshot()));return out.toString();
    }
    private static String encode(String text){return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));}
    public static EuropeanSeason restore(String text) {
        if(text==null||text.length()>196608)throw new IllegalArgumentException("Invalid European season size");
        String[] lines=text.split("\n",-1),h=lines[0].split("\\|",-1);
        if(lines.length!=2||h.length!=12||!h[0].equals("US1")||(!h[2].equals("0")&&!h[2].equals("1")))throw new IllegalArgumentException("Invalid European season save");
        ArrayList<LocalDate> dates=new ArrayList<>();for(int i=3;i<12;i++)dates.add(LocalDate.parse(h[i]));
        String[] state=lines[1].split("\\|",2);if(state.length!=2||(!state[0].equals("K")&&!state[0].equals("L")))throw new IllegalArgumentException("Invalid European season phase");
        String decoded=new String(Base64.getDecoder().decode(state[1]),StandardCharsets.UTF_8);
        EuropeanKnockout cup=state[0].equals("K")?EuropeanKnockout.restore(decoded):null;
        EuropeanLeaguePhase phase=cup==null?EuropeanLeaguePhase.restore(decoded):cup.leaguePhase();
        EuropeanSeason season=new EuropeanSeason(phase,Long.parseLong(h[1]),dates,h[2].equals("1"));
        if(cup!=null) {
            // Match the nested bracket's immutable draw seed, dates and phase against the outer season.
            String[] expected=season.knockout().snapshot().split("\n",-1),actual=cup.snapshot().split("\n",-1);
            for(int i=0;i<11;i++)if(!expected[i].equals(actual[i]))throw new IllegalArgumentException("Conflicting European season metadata");
            season.knockout=cup;
        }
        if(!text.equals(season.snapshot()))throw new IllegalArgumentException("Non-canonical European season save");return season;
    }
}
