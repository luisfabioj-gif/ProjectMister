package com.projectmister.game;

import java.util.*;

/** FIGC 25/A and 244/A, 2026/27. Includes Serie A title/survival deciders. */
public final class ItalianPromotion implements PromotionCampaign {
    private final int[] lower,points,upper;
    private final boolean titleTie,survivalTie,automaticThird;
    private final ArrayList<KnockoutTie> games=new ArrayList<>();
    private final int preliminaries;
    // upper: first, second, seventeenth, eighteenth, nineteenth, twentieth.
    public ItalianPromotion(int[] lowerEight,int[] lowerPoints,int[] upperSix,boolean titleTie,boolean survivalTie) {
        if(lowerEight.length!=8||lowerPoints.length!=8||upperSix.length!=6)throw new IllegalArgumentException("Wrong field size");
        Set<Integer> seen=new HashSet<>();for(int[] field:new int[][]{lowerEight,upperSix})for(int id:field)if(id<0||!seen.add(id))throw new IllegalArgumentException("Invalid entrant");
        for(int i=0;i<8;i++)if(lowerPoints[i]<-100||lowerPoints[i]>300||(i>0&&lowerPoints[i]>lowerPoints[i-1]))throw new IllegalArgumentException("Invalid points");
        lower=lowerEight.clone();points=lowerPoints.clone();upper=upperSix.clone();this.titleTie=titleTie;this.survivalTie=survivalTie;
        automaticThird=points[2]-points[3]>14;preliminaries=(titleTie?1:0)+(survivalTie?1:0);advance();
    }
    private int seed(int club){for(int i=0;i<8;i++)if(lower[i]==club)return i;throw new IllegalArgumentException("Unknown entrant");}
    private KnockoutTie lowerTie(int a,int b,int legs,KnockoutTie.Rule rule) {
        int high=seed(a)<seed(b)?a:b,low=high==a?b:a;
        return new KnockoutTie(legs==1?high:low,legs==1?low:high,legs,high,false,rule);
    }
    private int count(){return preliminaries+(automaticThird?0:5);}
    private void advance() {
        if(games.size()==count()||!games.isEmpty()&&games.get(games.size()-1).winner()<0)return;
        int i=games.size();
        if(titleTie&&i==0){games.add(new KnockoutTie(upper[0],upper[1],1,upper[0],false,KnockoutTie.Rule.PENALTIES));return;}
        if(survivalTie&&i==(titleTie?1:0)){games.add(new KnockoutTie(upper[3],upper[2],2,upper[2],false,KnockoutTie.Rule.PENALTIES));return;}
        switch(i-preliminaries) {
            case 0:games.add(lowerTie(lower[4],lower[7],1,KnockoutTie.Rule.EXTRA_TIME_HIGHER_SEED));break;
            case 1:games.add(lowerTie(lower[5],lower[6],1,KnockoutTie.Rule.EXTRA_TIME_HIGHER_SEED));break;
            case 2:games.add(lowerTie(lower[2],games.get(preliminaries+1).winner(),2,KnockoutTie.Rule.HIGHER_SEED));break;
            case 3:games.add(lowerTie(lower[3],games.get(preliminaries).winner(),2,KnockoutTie.Rule.HIGHER_SEED));break;
            default:int a=games.get(preliminaries+2).winner(),b=games.get(preliminaries+3).winner();
                games.add(lowerTie(a,b,2,points[seed(a)]==points[seed(b)]?KnockoutTie.Rule.EXTRA_TIME_PENALTIES:KnockoutTie.Rule.HIGHER_SEED));
        }
    }
    public String country(){return "IT";}
    public int[] automaticPromoted(){return automaticThird?new int[]{lower[0],lower[1],lower[2]}:new int[]{lower[0],lower[1]};}
    public int[] automaticRelegated(){return survivalTie?new int[]{upper[4],upper[5]}:new int[]{upper[3],upper[4],upper[5]};}
    public int[] upperEntrants(){return upper.clone();}
    public int[] lowerEntrants(){return lower.clone();}
    public String roundName(int i){if(titleTie&&i==0)return "Serie A title decider";if(i<preliminaries)return "Serie A survival decider";i-=preliminaries;return i<2?"Serie B preliminary "+(i+1):i<4?"Serie B semi-final "+(i-1):"Serie B promotion final";}
    public KnockoutTie current(){advance();return games.isEmpty()||games.size()==count()&&games.get(games.size()-1).winner()>=0?null:games.get(games.size()-1);}
    public boolean complete(){return current()==null;}
    public List<KnockoutTie> ties(){advance();return Collections.unmodifiableList(games);}
    public int champion(){if(!complete())throw new IllegalStateException("Unfinished season");return titleTie?games.get(0).winner():upper[0];}
    public int[] promoted(){if(!complete())throw new IllegalStateException("Unfinished promotion");return automaticThird?automaticPromoted():new int[]{lower[0],lower[1],games.get(preliminaries+4).winner()};}
    public int[] relegated(){if(!complete())throw new IllegalStateException("Unfinished relegation");return new int[]{survivalTie?games.get(titleTie?1:0).loser():upper[3],upper[4],upper[5]};}
    public String snapshot(){advance();StringBuilder s=new StringBuilder("IT#1;").append(titleTie?1:0).append(';').append(survivalTie?1:0);for(int[] a:new int[][]{lower,points,upper})for(int n:a)s.append(';').append(n);for(KnockoutTie t:games)s.append('#').append(t.snapshot());return s.toString();}
    public static ItalianPromotion restore(String text) {
        if(text==null||text.length()>4096)throw new IllegalArgumentException("Invalid promotion data");
        String[] rows=text.split("#",-1);if(rows.length<2||rows.length>9||!rows[0].equals("IT"))throw new IllegalArgumentException("Invalid campaign");
        String[] h=rows[1].split(";",-1);if(h.length!=25||!h[0].equals("1")||!h[1].matches("[01]")||!h[2].matches("[01]"))throw new IllegalArgumentException("Invalid header");
        int[] lower=new int[8],points=new int[8],upper=new int[6];int at=3;for(int[] a:new int[][]{lower,points,upper})for(int i=0;i<a.length;i++)a[i]=Integer.parseInt(h[at++]);
        ItalianPromotion p=new ItalianPromotion(lower,points,upper,h[1].equals("1"),h[2].equals("1"));
        if(rows.length==2&&p.current()!=null)throw new IllegalArgumentException("Missing fixture");
        for(int i=2;i<rows.length;i++) {
            KnockoutTie expected=p.current(),saved=KnockoutTie.restore(rows[i]);
            if(expected==null||saved.firstHome!=expected.firstHome||saved.firstAway!=expected.firstAway||saved.higherSeed!=expected.higherSeed||saved.legs!=expected.legs||saved.neutral||saved.rule!=expected.rule)throw new IllegalArgumentException("Altered fixture");
            p.games.set(i-2,saved);if(i<rows.length-1&&saved.winner()<0)throw new IllegalArgumentException("Incomplete earlier tie");
        }
        return p;
    }
}
