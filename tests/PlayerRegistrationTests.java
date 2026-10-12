package com.projectmister.game;
import java.time.LocalDate;

public final class PlayerRegistrationTests {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static PlayerRegistration.Decision decision(String country,int tier,String date){return PlayerRegistration.decide(country,tier,2026,LocalDate.parse(date),true,WorldMarket.FREE_AGENT_CONTRACT_END,false);}
    public static void main(String[] args) {
        for(String country:new String[]{"PT","DE","ENG","ES","IT","FR","NL","BE","SCO","TR"})for(int tier=1;tier<=2;tier++) {
            check(decision(country,tier,"2026-07-15").status==PlayerRegistration.Status.WINDOW,"Country ordinary summer window: "+country);
            check(!PlayerRegistration.decide(country,tier,2026,LocalDate.of(2026,10,15),false,null,false).permitted(),"Contracted off-window player rejected: "+country);
            check(!decision(country,tier,"2026-06-29").permitted(),"Contract has not expired: "+country);
            check(!PlayerRegistration.decide(country,tier,2026,LocalDate.of(2026,10,15),true,LocalDate.of(2026,6,30),true).permitted(),"Later registration disqualifies ordinary exception: "+country);
        }
        check(decision("PT",1,"2027-02-28").permitted()&&decision("PT",2,"2027-03-01").permitted()&&!decision("PT",1,"2027-03-02").permitted(),"Portuguese February deadline moves from Sunday to Monday");
        check(!PlayerRegistration.decide("PT",1,2026,LocalDate.of(2026,10,15),true,LocalDate.of(2026,8,31),false).permitted(),"Late expiry must not masquerade as proved mutual termination");
        check(decision("DE",1,"2026-12-31").permitted()&&decision("DE",1,"2027-01-01").status==PlayerRegistration.Status.WINDOW&&!decision("DE",1,"2027-02-02").permitted(),"German exemption stops before the winter window");
        check(decision("IT",1,"2027-03-31").permitted()&&!decision("IT",2,"2027-04-01").permitted(),"Italian final deadline inclusive");
        check(decision("BE",1,"2027-03-15").permitted()&&!decision("BE",2,"2027-03-16").permitted(),"Belgian final deadline inclusive");
        check(decision("ENG",2,"2027-03-25").permitted()&&!decision("ENG",2,"2027-03-26").permitted(),"EFL fourth Thursday cutoff");
        check(decision("ENG",1,"2027-05-01").requiresApproval(),"Premier League Board discretion explicit");
        check(decision("NL",1,"2026-10-15").requiresApproval(),"KNVB dispensation remains a simulated application");
        check(decision("SCO",1,"2027-05-01").permitted()&&decision("ES",1,"2027-05-01").permitted(),"Verified unregistered/expired-contract exceptions remain available");
        check(decision("FR",1,"2026-10-15").permitted()&&!decision("FR",2,"2027-02-02").permitted(),"French free-agent exemption ends at the winter deadline");
        check(!PlayerRegistration.decide("FR",1,2026,LocalDate.of(2026,10,15),true,LocalDate.of(2026,9,2),false).permitted(),"Late French release does not satisfy the exemption");
        check(!decision("TR",1,"2026-10-15").permitted(),"TFF ordinary unattached pool remains subject to its verified registration windows");
        PlayerRegistration.Decision leap=PlayerRegistration.decide("PT",1,2027,LocalDate.of(2028,2,29),true,WorldMarket.FREE_AGENT_CONTRACT_END,false);check(leap.permitted()&&leap.cutoff.equals(LocalDate.of(2028,2,29))&&leap.explanation.contains("projected"),"Projected leap-year cutoff labeled");
        check(!decision("PT",1,"2027-07-01").permitted(),"Completed season cannot authorize the next season");
        System.out.println("PASS: country/tier registration windows, expired-contract eligibility, free-agent cutoffs, projected leap years, explicit Board/KNVB approvals and unsupported-exception rejection");
    }
}
