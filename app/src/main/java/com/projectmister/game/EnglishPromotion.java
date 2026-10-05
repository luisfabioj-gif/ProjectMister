package com.projectmister.game;

import java.util.*;

/** EFL 2026/27: six playoff clubs, reseeded semifinals and neutral final. */
public final class EnglishPromotion implements PromotionCampaign {
    private final int[] lower,upper;
    private final ArrayList<KnockoutTie> games=new ArrayList<>();
    public EnglishPromotion(int[] lowerEight,int[] upperBottomThree) {
        if(lowerEight.length!=8||upperBottomThree.length!=3)throw new IllegalArgumentException("Wrong field size");
        Set<Integer> seen=new HashSet<>();
        for(int[] field:new int[][]{lowerEight,upperBottomThree})for(int id:field)
            if(id<0||!seen.add(id))throw new IllegalArgumentException("Invalid entrant");
        lower=lowerEight.clone();upper=upperBottomThree.clone();
        games.add(single(lower[4],lower[7]));
    }
    private KnockoutTie single(int home,int away){return new KnockoutTie(home,away,1,home,false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES);}
    private int seed(int club){for(int i=0;i<lower.length;i++)if(lower[i]==club)return i;throw new IllegalArgumentException("Unknown club");}
    private int quarterWinner(boolean lowest) {
        int a=games.get(0).winner(),b=games.get(1).winner();
        return (seed(a)>seed(b))==lowest?a:b;
    }
    private void advance() {
        KnockoutTie last=games.get(games.size()-1);
        if(last.winner()<0||games.size()==5)return;
        switch(games.size()) {
            case 1:games.add(single(lower[5],lower[6]));break;
            case 2:games.add(new KnockoutTie(quarterWinner(true),lower[2],2,lower[2],false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES));break;
            case 3:games.add(new KnockoutTie(quarterWinner(false),lower[3],2,lower[3],false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES));break;
            default:int a=games.get(2).winner(),b=games.get(3).winner();
                games.add(new KnockoutTie(a,b,1,seed(a)<seed(b)?a:b,true,KnockoutTie.Rule.EXTRA_TIME_PENALTIES));
        }
    }
    public String country(){return "ENG";}
    public int[] automaticPromoted(){return new int[]{lower[0],lower[1]};}
    public int[] automaticRelegated(){return upper.clone();}
    public int[] upperEntrants(){return upper.clone();}
    public int[] lowerEntrants(){return lower.clone();}
    public String roundName(int i){return i<2?"Championship quarter-final "+(i+1):i<4?"Championship semi-final "+(i-1):"Wembley promotion final";}
    public KnockoutTie current(){advance();KnockoutTie last=games.get(games.size()-1);return last.winner()>=0&&games.size()==5?null:last;}
    public boolean complete(){return current()==null;}
    public List<KnockoutTie> ties(){advance();return Collections.unmodifiableList(games);}
    public int[] promoted(){if(!complete())throw new IllegalStateException("Unfinished promotion");return new int[]{lower[0],lower[1],games.get(4).winner()};}
    public int[] relegated(){if(!complete())throw new IllegalStateException("Unfinished promotion");return upper.clone();}
    public String snapshot(){advance();StringBuilder s=new StringBuilder("ENG#1");for(int id:lower)s.append(';').append(id);for(int id:upper)s.append(';').append(id);for(KnockoutTie t:games)s.append('#').append(t.snapshot());return s.toString();}
    public static EnglishPromotion restore(String text) {
        if(text==null||text.length()>3072)throw new IllegalArgumentException("Invalid promotion data");
        String[] rows=text.split("#",-1);
        if(rows.length<3||rows.length>7||!rows[0].equals("ENG"))throw new IllegalArgumentException("Invalid campaign");
        String[] h=rows[1].split(";",-1);if(h.length!=12||!h[0].equals("1"))throw new IllegalArgumentException("Invalid header");
        int[] lower=new int[8],upper=new int[3];for(int i=0;i<8;i++)lower[i]=Integer.parseInt(h[i+1]);for(int i=0;i<3;i++)upper[i]=Integer.parseInt(h[i+9]);
        EnglishPromotion p=new EnglishPromotion(lower,upper);
        for(int i=2;i<rows.length;i++) {
            KnockoutTie expected=p.current(),saved=KnockoutTie.restore(rows[i]);
            if(expected==null||saved.firstHome!=expected.firstHome||saved.firstAway!=expected.firstAway||saved.higherSeed!=expected.higherSeed||saved.legs!=expected.legs||saved.neutral!=expected.neutral||saved.rule!=expected.rule)throw new IllegalArgumentException("Altered fixture");
            p.games.set(i-2,saved);if(i<rows.length-1&&saved.winner()<0)throw new IllegalArgumentException("Incomplete earlier tie");
        }
        return p;
    }
}
