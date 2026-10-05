package com.projectmister.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** TFF 1. Lig 2026/27, article 3. Entrants are stable club IDs in final league order. */
public final class TurkeyPromotion implements PromotionCampaign {
    private final int[] lower,down;
    private final ArrayList<KnockoutTie> games=new ArrayList<>();
    public TurkeyPromotion(int[] lowerSeven,int[] relegatedThree) {
        if(lowerSeven.length!=7||relegatedThree.length!=3)throw new IllegalArgumentException("Invalid Turkish field");
        HashSet<Integer> ids=new HashSet<>();
        for(int[] group:new int[][]{lowerSeven,relegatedThree})for(int id:group)if(id<0||!ids.add(id))throw new IllegalArgumentException("Duplicate entrant");
        lower=lowerSeven.clone();down=relegatedThree.clone();games.add(single(lower[3],lower[6],false));
    }
    private KnockoutTie single(int higher,int other,boolean neutral){return new KnockoutTie(higher,other,1,higher,neutral,KnockoutTie.Rule.EXTRA_TIME_PENALTIES);}
    private int seed(int club){for(int i=0;i<lower.length;i++)if(lower[i]==club)return i;throw new IllegalArgumentException("Unknown seed");}
    private void advance(){
        if(games.get(games.size()-1).winner()<0||games.size()==4)return;
        if(games.size()==1)games.add(single(lower[4],lower[5],false));
        else if(games.size()==2){int a=games.get(0).winner(),b=games.get(1).winner();int high=seed(a)<seed(b)?a:b,low=high==a?b:a;
            games.add(new KnockoutTie(low,high,2,high,false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES));}
        else games.add(single(lower[2],games.get(2).winner(),true));
    }
    public String country(){return "TR";}
    public int[] automaticPromoted(){return Arrays.copyOf(lower,2);}
    public int[] automaticRelegated(){return down.clone();}
    public int[] upperEntrants(){return down.clone();}
    public int[] lowerEntrants(){return lower.clone();}
    public String roundName(int index){return new String[]{"Eliminator • 4th v 7th","Eliminator • 5th v 6th","Two-leg semi-final","Promotion final • neutral venue"}[index];}
    public KnockoutTie current(){advance();KnockoutTie last=games.get(games.size()-1);return games.size()==4&&last.winner()>=0?null:last;}
    public boolean complete(){return current()==null;}
    public List<KnockoutTie> ties(){advance();return Collections.unmodifiableList(games);}
    public int[] promoted(){if(!complete())throw new IllegalStateException("Playoffs incomplete");return new int[]{lower[0],lower[1],games.get(3).winner()};}
    public int[] relegated(){if(!complete())throw new IllegalStateException("Playoffs incomplete");return down.clone();}
    public String snapshot(){advance();StringBuilder s=new StringBuilder("TR#1");for(int id:lower)s.append(';').append(id);for(int id:down)s.append(';').append(id);for(KnockoutTie t:games)s.append('#').append(t.snapshot());return s.toString();}
    public static TurkeyPromotion restore(String value){
        if(value==null||value.length()>3072)throw new IllegalArgumentException("Invalid Turkish data");
        String[] rows=value.split("#",-1);if(rows.length<3||rows.length>6||!rows[0].equals("TR"))throw new IllegalArgumentException("Invalid Turkish schema");
        String[] h=rows[1].split(";",-1);if(h.length!=11||!h[0].equals("1"))throw new IllegalArgumentException("Invalid Turkish header");
        int[] lower=new int[7],down=new int[3];for(int i=0;i<7;i++)lower[i]=Integer.parseInt(h[i+1]);for(int i=0;i<3;i++)down[i]=Integer.parseInt(h[i+8]);
        TurkeyPromotion p=new TurkeyPromotion(lower,down);
        for(int i=2;i<rows.length;i++){
            KnockoutTie expected=p.current(),saved=KnockoutTie.restore(rows[i]);
            if(expected==null||saved.firstHome!=expected.firstHome||saved.firstAway!=expected.firstAway||saved.legs!=expected.legs||saved.higherSeed!=expected.higherSeed||saved.neutral!=expected.neutral||saved.rule!=expected.rule)throw new IllegalArgumentException("Invalid Turkish fixture");
            p.games.set(i-2,saved);if(i<rows.length-1&&saved.winner()<0)throw new IllegalArgumentException("Unfinished round");
        }
        return p;
    }
}
