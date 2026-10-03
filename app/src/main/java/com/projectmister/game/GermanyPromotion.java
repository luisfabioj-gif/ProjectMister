package com.projectmister.game;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** DFL SpOL 11 June 2026 section 3: two automatic swaps and a two-leg tie.
 * Later final league fixture means fewer rest days and the return leg at home.
 * Equal dates require a saved draw, never a fixed preference for either tier. */
public final class GermanyPromotion implements PromotionCampaign {
    public final int champion,runnerUp,down17,down18,upper16,lower3;
    private KnockoutTie tie;
    public GermanyPromotion(int champion,int runnerUp,int down17,int down18,int upper16,int lower3,
                            LocalDate upperFinal,LocalDate lowerFinal,boolean upperWinsDraw) {
        HashSet<Integer> seen=new HashSet<>();
        for(int id:new int[]{champion,runnerUp,down17,down18,upper16,lower3})
            if(id<0||!seen.add(id))throw new IllegalArgumentException("Invalid German entrants");
        if(upperFinal==null||lowerFinal==null)throw new IllegalArgumentException("Missing final fixture date");
        this.champion=champion;this.runnerUp=runnerUp;this.down17=down17;this.down18=down18;this.upper16=upper16;this.lower3=lower3;
        boolean upperSecond=upperFinal.isAfter(lowerFinal)||(upperFinal.equals(lowerFinal)&&upperWinsDraw);
        tie=new KnockoutTie(upperSecond?lower3:upper16,upperSecond?upper16:lower3,2,upper16,false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES);
    }
    public String country(){return "DE";}
    public int[] automaticPromoted(){return new int[]{champion,runnerUp};}
    public int[] automaticRelegated(){return new int[]{down17,down18};}
    public int[] upperEntrants(){return new int[]{down17,down18,upper16};}
    public int[] lowerEntrants(){return new int[]{champion,runnerUp,lower3};}
    public String roundName(int index){if(index!=0)throw new IllegalArgumentException("Invalid round");return "Bundesliga relegation play-off";}
    public KnockoutTie current(){return complete()?null:tie;}
    public boolean complete(){return tie.winner()>=0;}
    public List<KnockoutTie> ties(){return Collections.singletonList(tie);}
    public int[] promoted(){if(!complete())throw new IllegalStateException("Playoff incomplete");return tie.winner()==upper16?automaticPromoted():new int[]{champion,runnerUp,lower3};}
    public int[] relegated(){if(!complete())throw new IllegalStateException("Playoff incomplete");return tie.winner()==upper16?automaticRelegated():new int[]{down17,down18,upper16};}
    public String snapshot(){return "DE#1;"+champion+";"+runnerUp+";"+down17+";"+down18+";"+upper16+";"+lower3+"#"+tie.snapshot();}
    public static GermanyPromotion restore(String value){
        if(value==null||value.length()>1024)throw new IllegalArgumentException("Invalid German playoff data");
        String[] rows=value.split("#",-1);if(rows.length!=3||!rows[0].equals("DE"))throw new IllegalArgumentException("Invalid German schema");
        String[] h=rows[1].split(";",-1);if(h.length!=7||!h[0].equals("1"))throw new IllegalArgumentException("Invalid German header");
        GermanyPromotion p=new GermanyPromotion(Integer.parseInt(h[1]),Integer.parseInt(h[2]),Integer.parseInt(h[3]),Integer.parseInt(h[4]),Integer.parseInt(h[5]),Integer.parseInt(h[6]),LocalDate.of(2000,1,1),LocalDate.of(2000,1,1),false);
        KnockoutTie saved=KnockoutTie.restore(rows[2]);
        if(saved.legs!=2||saved.neutral||saved.higherSeed!=p.upper16||saved.rule!=KnockoutTie.Rule.EXTRA_TIME_PENALTIES
            ||!((saved.firstHome==p.upper16&&saved.firstAway==p.lower3)||(saved.firstHome==p.lower3&&saved.firstAway==p.upper16)))throw new IllegalArgumentException("Invalid German fixture");
        p.tie=saved;return p;
    }
}
