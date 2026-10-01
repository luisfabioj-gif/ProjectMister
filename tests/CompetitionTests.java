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
            if(n%2==0) for(int club:ids) {
                int halfHomes=0,streak=0; Boolean previous=null;
                for(int r=0;r<s.roundCount();r++) {
                    boolean home=s.fixture(r,club).home==club;
                    if(r<n-1 && home)halfHomes++;
                    streak=previous!=null && previous==home?streak+1:1;
                    check(streak<=3,"excessive consecutive home/away fixtures");previous=home;
                }
                check(Math.abs(halfHomes*2-(n-1))<=1,"unbalanced first half-season venues");
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
        check(RegistrationWindow.forDivision("IT",1).get(0).includes(LocalDate.of(2026,6,29)),"Serie A early summer opening");
        check(!RegistrationWindow.forDivision("IT",2).get(0).includes(LocalDate.of(2026,6,29)),"Serie B must not inherit Serie A opening");
        check(RegistrationWindow.forDivision("IT",2).get(1).includes(LocalDate.of(2027,1,2)),"Italy winter opening");
        check(!RegistrationWindow.forDivision("IT",1).get(1).includes(ZonedDateTime.parse("2027-02-01T19:00:01Z")),"Italy local deadline");
        for(String country:new String[]{"ENG","ES","DE","IT","FR","PT","NL","BE","SCO","TR"}) {
            for(int tier=1;tier<=2;tier++) {
                java.util.List<RegistrationWindow> windows=RegistrationWindow.forDivision(country,tier);
                check(windows.size()==2,"missing division registration dates "+country+":"+tier);
                for(RegistrationWindow w:windows) {
                    check(w.includes(w.opens)&&w.includes(w.closes),"excluded registration day");
                    check(!w.includes(w.opens.minusDays(1))&&!w.includes(w.closes.plusDays(1)),"window spills outside dates");
                    check(!w.includes(w.opens.plusYears(1)),"future season uses stale dates");
                }
            }
        }
        check(RegistrationWindow.forCountry("SCO").get(1).opens.equals(LocalDate.of(2027,1,5)),"Scottish winter opening");
        check(RegistrationWindow.forCountry("ES").get(1).closes.equals(LocalDate.of(2027,2,1)),"Spanish winter close verified against FIFA");
        boolean unknownRejected=false;
        try { RegistrationWindow.forCountry("BE").get(0).includes(ZonedDateTime.parse("2026-09-03T12:00:00Z")); }
        catch(IllegalStateException expected){unknownRejected=true;}
        check(unknownRejected,"unknown deadline must not become midnight");
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
