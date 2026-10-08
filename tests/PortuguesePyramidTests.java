package com.projectmister.game;
import java.time.LocalDate;
import java.util.*;

public final class PortuguesePyramidTests {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static int[] members(int[] levels,int level){ArrayList<Integer> out=new ArrayList<>();for(int c=0;c<levels.length;c++)if(levels[c]==level)out.add(c);return out.stream().mapToInt(Integer::intValue).toArray();}
    static int[][] groups(int[] clubs,int count){int[][] out=new int[count][];int cursor=0;for(int g=0;g<count;g++){out[g]=new int[clubs.length/count+(g<clubs.length%count?1:0)];for(int i=0;i<out[g].length;i++)out[g][i]=clubs[cursor++];}return out;}
    static PortugueseThirdDivisionSeason.Scores scores(final long seed){return new PortugueseThirdDivisionSeason.Scores(){final Random random=new Random(seed);public int goals(int h,int a,boolean advantage){return random.nextInt(4);}public PortugueseLeagueCupSeason.PlayerUse[] players(int club){PortugueseLeagueCupSeason.PlayerUse[] used=new PortugueseLeagueCupSeason.PlayerUse[11];for(int i=0;i<11;i++)used[i]=new PortugueseLeagueCupSeason.PlayerUse(club*100+i,7300+club*10+i);return used;}};}
    public static void main(String[] args){
        int[] levels=new int[154];String[] ids=new String[154];boolean[] reserves=new boolean[154];for(int c=0;c<levels.length;c++){levels[c]=c<18?1:c<36?2:c<56?3:c<112?4:5;ids[c]="pt:test-"+c;}
        String[] parents={"fc-porto","sl-benfica","sporting-cp","vitoria-sc","sc-braga","gd-chaves","fc-alverca","santa-clara"};int[] parentIds={0,1,2,3,4,18,19,5},bIds={33,34,35,36,56,57,58,59};String[] bNames={"fc-porto-b","sl-benfica-b","sporting-cp-b","cup-vitoria-sc-b","cup-sc-braga-b","cup-gd-chaves-b","cup-fc-alverca-b","cup-santa-clara-b"};for(int i=0;i<parents.length;i++){ids[parentIds[i]]="pt:"+parents[i];ids[bIds[i]]="pt:"+bNames[i];reserves[bIds[i]]=true;}
        PortugueseThirdDivisionSeasons thirdHistory=null;PortugueseFourthDivisionSeasons fourthHistory=null;PortugueseDistrictSeasons districtHistory=null;
        for(int year=2026;year<2036;year++){
            int[][] thirdGroups=groups(members(levels,3),2);PortugueseThirdDivisionSeason third=new PortugueseThirdDivisionSeason(year,year,154,thirdGroups[0],thirdGroups[1]);PortugueseFourthDivisionSeason fourth=new PortugueseFourthDivisionSeason(year,year,154,groups(members(levels,4),4));PortugueseDistrictSeason district=new PortugueseDistrictSeason(year,year,154,groups(members(levels,5),4));
            LocalDate end=LocalDate.of(year+1,6,20);PortugueseThirdDivisionSeason.Scores scorer=scores(year);third.advanceTo(end,scorer);fourth.advanceTo(end,scorer);district.advanceTo(end,scorer);
            third=PortugueseThirdDivisionSeason.restore(third.snapshot());fourth=PortugueseFourthDivisionSeason.restore(fourth.snapshot());district=PortugueseDistrictSeason.restore(district.snapshot());check(third.complete()&&fourth.complete()&&district.complete(),"All lower qualification seasons complete");check(district.promoted().length==20,"Twenty result-derived district qualifiers");district.validateDate(end);
            if(thirdHistory==null){thirdHistory=new PortugueseThirdDivisionSeasons(third);fourthHistory=new PortugueseFourthDivisionSeasons(fourth);districtHistory=new PortugueseDistrictSeasons(district);}else{thirdHistory=thirdHistory.next(third);fourthHistory=fourthHistory.next(fourth);districtHistory=districtHistory.next(district);}
            thirdHistory=PortugueseThirdDivisionSeasons.restore(thirdHistory.snapshot());fourthHistory=PortugueseFourthDivisionSeasons.restore(fourthHistory.snapshot());districtHistory=PortugueseDistrictSeasons.restore(districtHistory.snapshot());
            int[] second=members(levels,2),top=members(levels,1);int[] eligible=Arrays.stream(second).filter(c->!reserves[c]).toArray();int[] upperUp=Arrays.copyOf(eligible,2),upperDown=new int[]{top[0],top[top.length-1]};
            PortugueseLowerPromotion lower=new PortugueseLowerPromotion(year,Arrays.copyOf(third.promotionOrder(),3),Arrays.copyOfRange(second,15,18),false);
            while(!lower.complete())PortugueseLowerDeciders.simulate(lower.current(),scorer,year);
            PortuguesePyramidOutcome next=new PortuguesePyramidOutcome(year,ids,levels,reserves,upperUp,upperDown,lower,third.relegated(),fourth.promoted(),fourth.relegated(),district.promoted(),second,third.sportingOrder(),fourth.sportingOrder(),district.sportingOrder());
            check(members(next.levels,1).length==18&&members(next.levels,2).length==18&&members(next.levels,3).length==20&&members(next.levels,4).length==64,"All divisions keep the published/projected field size");for(int i=0;i<bIds.length;i++)check(next.levels[bIds[i]]>next.levels[parentIds[i]],"Every reserve stays below its parent");check(Arrays.stream(bIds).filter(c->next.levels[c]==2).count()<=5,"Liga 2 maximum five reserves");
            int[] direct=Arrays.stream(members(next.levels,1)).filter(c->!reserves[c]).limit(6).toArray();PortugueseLeagueCupSeason cup=PortugueseLeagueCupCareer.create(year+1,year,ids,next.levels,reserves,direct,PortugueseLeagueCupCareer.preliminary(lower,next.levels,reserves,direct,second));check(cup.clubs().length==36-Arrays.stream(bIds).filter(c->next.levels[c]<=2).count(),"Actual next professional cup field excludes B teams");for(int c:cup.preliminary())check(next.levels[c]<=2&&!reserves[c],"Actual eligible preliminary participants");levels=next.levels;
        }
        check(thirdHistory.archives().size()==9&&fourthHistory.archives().size()==9&&districtHistory.archives().size()==9,"Lower histories retained for ten seasons");
        System.out.println("PASS: ten complete Portuguese pyramid seasons, result-led district qualifiers, 56→64 expansion, parent/reserve vacancy rulings, maximum five Liga 2 B teams, actual eligible next League Cup entrants and canonical archives");
    }
}
