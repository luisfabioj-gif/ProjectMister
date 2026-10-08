package com.projectmister.game;
import java.time.LocalDate;
import java.util.*;
public final class PortugueseThirdDivisionTests {
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    private static void reject(Runnable r){try{r.run();}catch(RuntimeException expected){return;}throw new AssertionError("Invalid Liga 3 save accepted");}
    public static void main(String[] args) {
        int[] north=new int[10],south=new int[10];for(int i=0;i<10;i++){north[i]=710+i;south[i]=720+i;}
        for(int edition=0;edition<18;edition++) {
            final int season=2026+edition;Random random=new Random(733+edition);PortugueseThirdDivisionSeason cup=new PortugueseThirdDivisionSeason(season,77+edition,768,north,south);
            PortugueseThirdDivisionSeason.Scores scores=new PortugueseThirdDivisionSeason.Scores(){public int goals(int h,int a,boolean home){return random.nextInt(4);}public PortugueseLeagueCupSeason.PlayerUse[] players(int club){return new PortugueseLeagueCupSeason.PlayerUse[]{new PortugueseLeagueCupSeason.PlayerUse(club*20,8000+club)};}};
            for(int r=0;r<18;r++){cup.advanceTo(cup.firstDate(r),scores);check(cup.firstRounds()==r+1,"Regional round missing");cup=PortugueseThirdDivisionSeason.restore(cup.snapshot());}
            check(cup.laterGroup(0).length==8&&cup.laterGroup(1).length==6&&cup.laterGroup(2).length==6,"Wrong second-phase field");int[] regional=cup.order(0,0);
            for(int r=0;r<14;r++){cup.advanceTo(cup.secondDate(r),scores);cup.validateDate(cup.secondDate(r));cup=PortugueseThirdDivisionSeason.restore(cup.snapshot());}
            check(cup.complete()&&cup.results(0).size()==180&&cup.results(1).size()==116,"Incomplete Liga 3 season");
            check(Arrays.equals(regional,cup.order(0,0)),"Later cards/players alter original qualification");Set<Integer> relegated=new HashSet<>();for(int c:cup.relegated())relegated.add(c);check(cup.promotionOrder().length==8&&relegated.size()==4,"Wrong movements");
            String complete=cup.snapshot();reject(()->PortugueseThirdDivisionSeason.restore(complete+"\n"+Base64.getEncoder().encodeToString("F0|710|711|1;710;711;1;710;1;EXTRA_TIME_PENALTIES|R,1,0".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
            final PortugueseThirdDivisionSeason finished=cup;reject(()->finished.validateDate(LocalDate.of(season,9,1)));
        }
        check(PortugueseThirdDivisionSeason.survivalBonus(5,9)==0&&PortugueseThirdDivisionSeason.survivalBonus(10,14)==1&&PortugueseThirdDivisionSeason.survivalBonus(5,15)==7&&PortugueseThirdDivisionSeason.survivalBonus(5,30)==10,"Wrong survival bonuses");
        LeagueResults phase=new LeagueResults(20,true),previous=new LeagueResults(20,true);phase.record(0,0,1,0,0);phase.record(1,1,0,0,0);previous.record(0,0,1,2,0);int[] zeros=new int[20];long[] ages=new long[20];int[] uses=new int[20];
        PortugueseThirdDivisionStandings ranks=new PortugueseThirdDivisionStandings(new int[]{1,0},phase,previous,zeros,zeros,zeros,ages,uses);check(ranks.order[0]==0&&!ranks.tiedAt(0),"Whole-season head-to-head criterion missing");
        ages[0]=8000;ages[1]=9000;uses[0]=uses[1]=1;ranks=new PortugueseThirdDivisionStandings(new int[]{1,0},phase,null,zeros,zeros,zeros,ages,uses);check(ranks.order[0]==0&&!ranks.tiedAt(0),"Actual participant age criterion missing");
        System.out.println("PASS: 18 Liga 3 seasons, 296 results each, regional qualification, survival bonuses, phase/whole-season ranking, used-player ages, canonical saves and corruption rejection");
    }
}
