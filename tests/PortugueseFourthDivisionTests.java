package com.projectmister.game;
import java.time.LocalDate;
import java.util.*;
public final class PortugueseFourthDivisionTests {
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static void reject(Runnable r){try{r.run();}catch(RuntimeException expected){return;}throw new AssertionError("Invalid lower season accepted");}
    public static void main(String[] args) {
        for(int year=2026;year<2038;year++) {
            int n=year==2026?14:16;int[][] groups=new int[4][n];for(int g=0;g<4;g++)for(int i=0;i<n;i++)groups[g][i]=600+g*n+i;Random random=new Random(year);
            PortugueseThirdDivisionSeason.Scores scores=new PortugueseThirdDivisionSeason.Scores(){public int goals(int h,int a,boolean home){return random.nextInt(5);}public PortugueseLeagueCupSeason.PlayerUse[] players(int c){return new PortugueseLeagueCupSeason.PlayerUse[]{new PortugueseLeagueCupSeason.PlayerUse(c*20,8000+c)};}};
            PortugueseFourthDivisionSeason cup=new PortugueseFourthDivisionSeason(year,year,768,groups);
            for(int r=0;r<2*(n-1);r++){cup.advanceTo(cup.firstDate(r),scores);cup.validateDate(cup.firstDate(r));cup=PortugueseFourthDivisionSeason.restore(cup.snapshot());}
            check(cup.results(0).size()==4*n*(n-1),"Incomplete regional league");
            for(int r=0;r<6;r++){cup.advanceTo(cup.secondDate(r),scores);cup.validateDate(cup.secondDate(r));cup=PortugueseFourthDivisionSeason.restore(cup.snapshot());}
            check(cup.promoted().length==4&&cup.results(1).size()==24&&!cup.complete(),"Wrong lower promotion/final");
            cup.advanceTo(cup.finalDate(),scores);cup=PortugueseFourthDivisionSeason.restore(cup.snapshot());check(cup.complete()&&cup.winner()>=0&&cup.relegated().length==(year==2026?12:20),"Wrong lower relegation");String saved=cup.snapshot();reject(()->PortugueseFourthDivisionSeason.restore(saved+"\nYg=="));
        }
        System.out.println("PASS: 12 result-led Campeonato de Portugal seasons, 56/64-club fields, four promoted clubs, survival legs, neutral championship, phase saves and corruption rejection");
    }
}
