package com.projectmister.game;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
public final class CompetitionTests {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        for (int n : new int[]{10,12,15,18,20,22,24}) for (int legs : new int[]{2,4}) {
            int[] ids = new int[n]; for(int i=0;i<n;i++) ids[i]=100+i*3;
            LeagueSchedule s = new LeagueSchedule(ids, legs);
            int[][] meetings = new int[n][n]; int[] homes=new int[n], aways=new int[n], byes=new int[n];
            for (int r=0;r<s.roundCount();r++) {
                HashSet<Integer> seen = new HashSet<>();
                for (LeagueSchedule.Pairing p : s.round(r)) {
                    check(seen.add(p.home)&&seen.add(p.away), "club plays twice in a round");
                    int h=(p.home-100)/3,a=(p.away-100)/3;
                    check(h!=a,"self fixture"); meetings[h][a]++;homes[h]++;aways[a]++;
                    check(s.fixture(r,p.away).opponent(p.away)==p.home,"calendar/live disagreement");
                }
                for(int i=0;i<n;i++) if(!seen.contains(ids[i])) {byes[i]++;check(s.fixture(r,ids[i])==null,"invented bye opponent");}
            }
            for(int i=0;i<n;i++) {
                check(homes[i]==(n-1)*legs/2 && homes[i]==aways[i],"unbalanced home/away games");
                check(byes[i]==(n%2==1?legs:0),"incorrect byes");
                for(int j=0;j<n;j++) check(meetings[i][j]==(i==j?0:legs/2),"missing or repeated fixture");
            }
        }
        for (RegistrationWindow w : RegistrationWindow.forCountry("PT")) {
            check(!w.includes(w.opens.minusDays(1)) && w.includes(w.opens),"opening boundary");
            check(w.includes(w.closes) && !w.includes(w.closes.plusDays(1)),"closing boundary");
            check(!w.includes(w.opens.plusYears(1)),"old dates reused in future season");
        }
        RegistrationWindow eng = RegistrationWindow.forCountry("ENG").get(0);
        check(eng.includes(ZonedDateTime.parse("2026-09-01T22:00:00Z")),"BST closing instant");
        check(!eng.includes(ZonedDateTime.parse("2026-09-01T22:00:01Z")),"late registration");
        check(RegistrationWindow.forCountry("UNVERIFIED").isEmpty(),"invented window");
        check(RegistrationWindow.forCountry("PT").get(1).includes(LocalDate.of(2027,1,4)),"Portuguese winter opening");
        check(!RegistrationWindow.forCountry("PT").get(1).includes(LocalDate.of(2027,1,1)),"countries share wrong date");
        check(EuropeanAccess.positionLabel("PT",3).contains("Q3"),"Portugal third CL place");
        check(EuropeanAccess.positionLabel("NL",2).contains("Q3"),"Dutch access reduced");
        check(EuropeanAccess.positionLabel("SCO",1).contains("Q2"),"Scottish access");
        check(EuropeanAccess.positionLabel("ENG",5).contains("Europa"),"unearned permanent EPS");
        check(EuropeanAccess.positionLabel("PT",6).contains("cup-dependent"),"cup winner assumed");
        System.out.println("PASS: 14 fixture formats, byes, registration boundaries, provisional UEFA access");
    }
}
