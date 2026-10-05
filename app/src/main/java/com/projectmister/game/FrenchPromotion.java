package com.projectmister.game;

import java.util.*;

/** LFP 2026/27 arts.519/519 ter: two single games, then a two-leg barrage. */
public final class FrenchPromotion implements PromotionCampaign {
    private final int[] lower,upper;
    private final ArrayList<KnockoutTie> games=new ArrayList<>();
    public FrenchPromotion(int[] lowerFive,int[] upperBottomThree) {
        if(lowerFive.length!=5||upperBottomThree.length!=3)throw new IllegalArgumentException("Wrong field size");
        Set<Integer> seen=new HashSet<>();
        for(int[] field:new int[][]{lowerFive,upperBottomThree})for(int id:field)
            if(id<0||!seen.add(id))throw new IllegalArgumentException("Invalid entrant");
        lower=lowerFive.clone();upper=upperBottomThree.clone();
        games.add(single(lower[3],lower[4]));
    }
    private KnockoutTie single(int home,int away){return new KnockoutTie(home,away,1,home,false,KnockoutTie.Rule.PENALTIES);}
    private void advance() {
        KnockoutTie last=games.get(games.size()-1);
        if(last.winner()<0||games.size()==3)return;
        games.add(games.size()==1?single(lower[2],last.winner()):
            new KnockoutTie(last.winner(),upper[0],2,upper[0],false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES));
    }
    public String country(){return "FR";}
    public int[] automaticPromoted(){return new int[]{lower[0],lower[1]};}
    public int[] automaticRelegated(){return new int[]{upper[1],upper[2]};}
    public int[] upperEntrants(){return upper.clone();}
    public int[] lowerEntrants(){return lower.clone();}
    public String roundName(int i){return i<2?"Ligue 2 play-off "+(i+1):"Promotion / relegation barrage";}
    public KnockoutTie current(){advance();KnockoutTie last=games.get(games.size()-1);return last.winner()>=0&&games.size()==3?null:last;}
    public boolean complete(){return current()==null;}
    public List<KnockoutTie> ties(){advance();return Collections.unmodifiableList(games);}
    public int[] promoted(){if(!complete())throw new IllegalStateException("Unfinished promotion");return games.get(2).winner()==upper[0]?automaticPromoted():new int[]{lower[0],lower[1],games.get(2).winner()};}
    public int[] relegated(){if(!complete())throw new IllegalStateException("Unfinished promotion");return games.get(2).winner()==upper[0]?automaticRelegated():upper.clone();}
    public String snapshot(){advance();StringBuilder s=new StringBuilder("FR#1");for(int id:lower)s.append(';').append(id);for(int id:upper)s.append(';').append(id);for(KnockoutTie t:games)s.append('#').append(t.snapshot());return s.toString();}
    public static FrenchPromotion restore(String text) {
        if(text==null||text.length()>3072)throw new IllegalArgumentException("Invalid promotion data");
        String[] rows=text.split("#",-1);
        if(rows.length<3||rows.length>5||!rows[0].equals("FR"))throw new IllegalArgumentException("Invalid campaign");
        String[] h=rows[1].split(";",-1);if(h.length!=9||!h[0].equals("1"))throw new IllegalArgumentException("Invalid header");
        int[] lower=new int[5],upper=new int[3];for(int i=0;i<5;i++)lower[i]=Integer.parseInt(h[i+1]);for(int i=0;i<3;i++)upper[i]=Integer.parseInt(h[i+6]);
        FrenchPromotion p=new FrenchPromotion(lower,upper);
        for(int i=2;i<rows.length;i++) {
            KnockoutTie expected=p.current(),saved=KnockoutTie.restore(rows[i]);
            if(expected==null||saved.firstHome!=expected.firstHome||saved.firstAway!=expected.firstAway||saved.higherSeed!=expected.higherSeed||saved.legs!=expected.legs||saved.neutral||saved.rule!=expected.rule)throw new IllegalArgumentException("Altered fixture");
            p.games.set(i-2,saved);if(i<rows.length-1&&saved.winner()<0)throw new IllegalArgumentException("Incomplete earlier tie");
        }
        return p;
    }
}
