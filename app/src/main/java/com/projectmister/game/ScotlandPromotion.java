package com.projectmister.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** SPFL Premiership/Championship ladder, C18–C25 (29 July 2026).
 * League ranking and lower-pyramid relegation are separate responsibilities. */
public final class ScotlandPromotion implements PromotionCampaign {
    public final int automaticUp,automaticDown,premiershipClub,second,third,fourth;
    private final ArrayList<KnockoutTie> ties=new ArrayList<>();
    public ScotlandPromotion(int automaticUp,int automaticDown,int premiershipClub,int second,int third,int fourth) {
        HashSet<Integer> seen=new HashSet<>();
        for(int id:new int[]{automaticUp,automaticDown,premiershipClub,second,third,fourth})
            if(id<0||!seen.add(id))throw new IllegalArgumentException("Invalid promotion entrants");
        this.automaticUp=automaticUp;this.automaticDown=automaticDown;this.premiershipClub=premiershipClub;
        this.second=second;this.third=third;this.fourth=fourth;ties.add(newTie(fourth,third));
    }
    public String country(){return "SCO";}
    public int[] automaticPromoted(){return new int[]{automaticUp};}
    public int[] automaticRelegated(){return new int[]{automaticDown};}
    public int[] upperEntrants(){return new int[]{automaticDown,premiershipClub};}
    public int[] lowerEntrants(){return new int[]{automaticUp,second,third,fourth};}
    public String roundName(int index){return new String[]{"Quarter-final","Semi-final","Premiership play-off final"}[index];}
    private static KnockoutTie newTie(int lower,int higher){return new KnockoutTie(lower,higher,2,higher,false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES);}
    private void advance() {
        KnockoutTie last=ties.get(ties.size()-1);
        if(last.winner()<0||ties.size()==3)return;
        ties.add(newTie(last.winner(),ties.size()==1?second:premiershipClub));
    }
    public KnockoutTie current(){advance();KnockoutTie last=ties.get(ties.size()-1);return ties.size()==3&&last.winner()>=0?null:last;}
    public boolean complete(){return current()==null;}
    public List<KnockoutTie> ties(){advance();return Collections.unmodifiableList(ties);}
    public int[] promoted(){if(!complete())throw new IllegalStateException("Playoffs incomplete");int w=ties.get(2).winner();return w==premiershipClub?new int[]{automaticUp}:new int[]{automaticUp,w};}
    public int[] relegated(){if(!complete())throw new IllegalStateException("Playoffs incomplete");return ties.get(2).winner()==premiershipClub?new int[]{automaticDown}:new int[]{automaticDown,premiershipClub};}
    public String snapshot() {
        advance();StringBuilder s=new StringBuilder("1;").append(automaticUp).append(';').append(automaticDown).append(';').append(premiershipClub)
                .append(';').append(second).append(';').append(third).append(';').append(fourth);
        for(KnockoutTie t:ties)s.append('#').append(t.snapshot());return s.toString();
    }
    public static ScotlandPromotion restore(String value) {
        if(value==null||value.length()>2048)throw new IllegalArgumentException("Invalid promotion data");
        String[] rows=value.split("#",-1),h=rows[0].split(";",-1);
        if(h.length!=7||!h[0].equals("1")||rows.length<2||rows.length>4)throw new IllegalArgumentException("Invalid promotion schema");
        ScotlandPromotion p=new ScotlandPromotion(Integer.parseInt(h[1]),Integer.parseInt(h[2]),Integer.parseInt(h[3]),Integer.parseInt(h[4]),Integer.parseInt(h[5]),Integer.parseInt(h[6]));
        for(int i=1;i<rows.length;i++) {
            KnockoutTie expected=p.current(),saved=KnockoutTie.restore(rows[i]);
            if(expected==null||expected.firstHome!=saved.firstHome||expected.firstAway!=saved.firstAway||saved.legs!=2||saved.neutral
                    ||saved.higherSeed!=expected.higherSeed||saved.rule!=expected.rule)throw new IllegalArgumentException("Invalid promotion fixture");
            p.ties.set(i-1,saved);
            if(i<rows.length-1&&saved.winner()<0)throw new IllegalArgumentException("Unfinished promotion round");
        }
        return p;
    }
}
