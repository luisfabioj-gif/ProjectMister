package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** Liga 3 -> Liga 2 transition from supplied final eligible standings, including the saved barrage. */
public final class PortugueseLowerPromotion {
    public final int season;
    private final int[] third,second;
    private final boolean secondHomeFirst;
    private KnockoutTie playoff;

    /** third: eligible Liga 3 first/second/third; second: Liga 2 sixteenth/seventeenth/eighteenth.
     * Final rankings, licensing and reserve/parent eligibility must be established before construction. */
    public PortugueseLowerPromotion(int season,int[] third,int[] second,boolean secondHomeFirst) {
        if(season<2026||season>2200||third==null||third.length!=3||second==null||second.length!=3)throw new IllegalArgumentException("Invalid lower promotion field");
        this.season=season;this.third=third.clone();this.second=second.clone();this.secondHomeFirst=secondHomeFirst;
        Set<Integer> ids=new HashSet<>();for(int[] field:new int[][]{third,second})for(int c:field)if(c<0||!ids.add(c))throw new IllegalArgumentException("Invalid lower promotion club");
        if(season==2026&&secondHomeFirst)throw new IllegalArgumentException("2026/27 published barrage has Liga 3 at home first");
        playoff=new KnockoutTie(secondHomeFirst?second[0]:third[2],secondHomeFirst?third[2]:second[0],2,second[0],false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES);
    }
    public KnockoutTie current(){return complete()?null:playoff;}
    public boolean complete(){return playoff.winner()>=0;}
    public int[] promoted(){if(!complete())throw new IllegalStateException("Finish Liga 2 barrage");return playoff.winner()==second[0]?Arrays.copyOf(third,2):third.clone();}
    public int[] relegated(){if(!complete())throw new IllegalStateException("Finish Liga 2 barrage");return playoff.winner()==second[0]?new int[]{second[1],second[2]}:second.clone();}
    public int secondPromoted(){if(!complete())throw new IllegalStateException("Finish Liga 2 barrage");return third[1];}
    public int playoffWinner(){if(!complete())throw new IllegalStateException("Finish Liga 2 barrage");return playoff.winner();}
    /** The surviving Liga 2 club ranks above a promoted Liga 3 club; otherwise Liga 3 second ranks above third. */
    public int[] leagueCupPreliminary(){int winner=playoffWinner();return winner==second[0]?new int[]{winner,third[1]}:new int[]{third[1],winner};}
    public int[] nextProfessionalField(int[] previous,boolean[] reserves) {
        if(previous==null||reserves==null)throw new IllegalArgumentException("Missing professional field");
        Set<Integer> field=new TreeSet<>();for(int c:previous)if(c<0||c>=reserves.length||!field.add(c))throw new IllegalArgumentException("Invalid professional field");
        for(int c:second)if(!field.contains(c))throw new IllegalArgumentException("Barrage field does not match Liga 2");
        for(int c:third)if(c>=reserves.length||field.contains(c))throw new IllegalArgumentException("Liga 3 entrant already professional");
        for(int c:relegated())field.remove(c);for(int c:promoted())field.add(c);
        field.removeIf(c->reserves[c]);int[] next=new int[field.size()];int i=0;for(int c:field)next[i++]=c;return next;
    }
    public PortugueseLeagueCupSeason nextLeagueCup(long seed,int[] previousProfessional,boolean[] reserves,int[] direct,List<LocalDate> dates,boolean simulatedDates) {
        int[] next=nextProfessionalField(previousProfessional,reserves);
        if(direct==null)throw new IllegalArgumentException("Missing direct qualifiers");
        int[] preliminary=(next.length-direct.length)%2==1?leagueCupPreliminary():new int[0];
        return new PortugueseLeagueCupSeason(season+1,seed,next,direct,preliminary,dates,simulatedDates);
    }
    public String snapshot(){StringBuilder out=new StringBuilder("PTLOW1|").append(season).append('|').append(secondHomeFirst?1:0);for(int[] field:new int[][]{third,second})for(int c:field)out.append('|').append(c);return out.append('\n').append(playoff.snapshot()).toString();}
    public static PortugueseLowerPromotion restore(String text) {
        if(text==null||text.length()>1024)throw new IllegalArgumentException("Invalid lower promotion save size");
        String[] rows=text.split("\n",-1),h=rows[0].split("\\|",-1);
        if(rows.length!=2||h.length!=9||!h[0].equals("PTLOW1")||!h[2].equals("0")&&!h[2].equals("1"))throw new IllegalArgumentException("Invalid lower promotion save");
        int[] lower=new int[3],upper=new int[3];for(int i=0;i<3;i++){lower[i]=Integer.parseInt(h[3+i]);upper[i]=Integer.parseInt(h[6+i]);}
        PortugueseLowerPromotion result=new PortugueseLowerPromotion(Integer.parseInt(h[1]),lower,upper,h[2].equals("1"));KnockoutTie tie=KnockoutTie.restore(rows[1]),expected=result.playoff;
        if(tie.firstHome!=expected.firstHome||tie.firstAway!=expected.firstAway||tie.higherSeed!=expected.higherSeed||tie.legs!=2||tie.neutral||tie.rule!=expected.rule)throw new IllegalArgumentException("Altered Liga 2 barrage");
        result.playoff=tie;if(!text.equals(result.snapshot()))throw new IllegalArgumentException("Non-canonical lower promotion save");return result;
    }
}
