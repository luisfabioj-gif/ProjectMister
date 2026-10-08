package com.projectmister.game;
import java.util.*;

/** Apply completed sporting outcomes, then reserve/parent restrictions and ranked vacancy replacements. */
public final class PortuguesePyramidOutcome {
    public final int[] levels;
    public final List<String> rulings;
    public PortuguesePyramidOutcome(int year,String[] identities,int[] original,boolean[] reserves,int[] upperUp,int[] upperDown,
            PortugueseLowerPromotion lower,int[] thirdDown,int[] fourthUp,int[] fourthDown,int[] districtUp,
            int[] secondOrder,int[] thirdOrder,int[] fourthOrder,int[] districtOrder) {
        if(year<2026||identities==null||original==null||reserves==null||identities.length!=original.length||reserves.length!=original.length||lower==null||!lower.complete())throw new IllegalArgumentException("Incomplete Portuguese season outcomes");levels=original.clone();ArrayList<String> rulings=new ArrayList<>();
        move(identities,upperUp,2,1);move(identities,upperDown,1,2);move(identities,lower.promoted(),3,2);move(identities,lower.relegated(),2,3);move(identities,fourthUp,4,3);move(identities,thirdDown,3,4);move(identities,districtUp,5,4);move(identities,fourthDown,4,5);
        boolean ineligiblePromoted=false;for(int c=0;c<levels.length;c++)if(reserves[c]&&identities[c].startsWith("pt:")){String parent=ReserveEligibility.parent(identities[c]);for(int p=0;p<levels.length;p++)if(identities[p].equals(parent)&&levels[c]<=levels[p]){if(original[c]==3&&levels[c]==2)ineligiblePromoted=true;levels[c]=levels[p]+1;rulings.add(identities[c]+" follows its parent's relegation or loses promotion eligibility");}}
        ArrayList<Integer> reserveOrder=combined(secondOrder,thirdOrder);int retained=0;for(int c:reserveOrder)if(reserves[c]&&levels[c]==2&&++retained>5){levels[c]=3;ineligiblePromoted=true;rulings.add(identities[c]+" exceeds the five-reserve-team limit");}
        ArrayList<Integer> second=combined(secondOrder,thirdOrder);if(ineligiblePromoted){second.remove((Integer)lower.tie().loser());second.add(0,lower.tie().loser());}
        fill(identities,reserves,2,18,second,rulings);fill(identities,reserves,3,20,combined(thirdOrder,fourthOrder),rulings);fill(identities,reserves,4,64,combined(fourthOrder,districtOrder),rulings);
        for(int c=0;c<levels.length;c++)if(reserves[c]&&identities[c].startsWith("pt:")&&!eligible(identities,c,levels[c]))throw new IllegalArgumentException("Reserve still shares its parent's division");
        this.rulings=Collections.unmodifiableList(rulings);
    }
    private void move(String[] identities,int[] clubs,int from,int to){Set<Integer> seen=new HashSet<>();for(int c:clubs)if(c<0||c>=levels.length||!identities[c].startsWith("pt:")||levels[c]!=from||!seen.add(c))throw new IllegalArgumentException("Invalid pyramid movement");else levels[c]=to;}
    private static ArrayList<Integer> combined(int[] first,int[] second){ArrayList<Integer> out=new ArrayList<>();for(int c:first)if(!out.contains(c))out.add(c);for(int c:second)if(!out.contains(c))out.add(c);return out;}
    private boolean eligible(String[] ids,int club,int target){if(target==1&&!ReserveEligibility.parent(ids[club]).isEmpty())return false;String parent=ReserveEligibility.parent(ids[club]);if(parent.isEmpty())return true;for(int p=0;p<ids.length;p++)if(ids[p].equals(parent))return levels[p]<target;return false;}
    private void fill(String[] ids,boolean[] reserves,int target,int expected,List<Integer> ranked,List<String> decisions) {
        int count=0;for(int c=0;c<levels.length;c++)if(ids[c].startsWith("pt:")&&levels[c]==target)count++;
        if(count>expected)throw new IllegalArgumentException("Too many clubs in Portuguese level "+target);
        for(int c:ranked)if(count<expected&&levels[c]==target+1&&eligible(ids,c,target)&&(!reserves[c]||target!=2||reserveCount(ids)<5)){levels[c]=target;count++;decisions.add(ids[c]+" fills a vacancy by sporting rank");}
        if(count!=expected)throw new IllegalArgumentException("Unfilled Portuguese level "+target);
    }
    private int reserveCount(String[] ids){int count=0;for(int c=0;c<levels.length;c++)if(ids[c].startsWith("pt:")&&levels[c]==2&&!ReserveEligibility.parent(ids[c]).isEmpty())count++;return count;}
}
