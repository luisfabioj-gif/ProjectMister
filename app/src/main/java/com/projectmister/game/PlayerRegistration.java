package com.projectmister.game;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Country registration rules for ordinary unattached professionals, with explicit game approvals. */
public final class PlayerRegistration {
    public enum Status { WINDOW, FREE_AGENT, EXEMPTION_APPLICATION, CLOSED }
    public static final class Decision {
        public final Status status;
        public final String explanation,source;
        public final LocalDate cutoff;
        Decision(Status status,String explanation,String source,LocalDate cutoff){this.status=status;this.explanation=explanation;this.source=source;this.cutoff=cutoff;}
        public boolean permitted(){return status!=Status.CLOSED;}
        public boolean requiresApproval(){return status==Status.EXEMPTION_APPLICATION;}
    }
    private PlayerRegistration(){}
    public static Decision decide(String country,int tier,int year,LocalDate date,boolean free,LocalDate contractEnded,boolean registeredSinceRelease) {
        if(country==null||date==null||tier<1||tier>2||year<2026||year>2200)throw new IllegalArgumentException("Invalid registration inputs");
        List<RegistrationWindow> windows=RegistrationWindow.forCareerSeason(country,tier,year);
        if(windows.size()!=2)return closed("Registration dates have not been verified for this association","",null);
        LocalDate seasonEnd=LocalDate.of(year+1,6,30);
        if(date.isBefore(LocalDate.of(year,6,1))||date.isAfter(seasonEnd))return closed("Date is outside this career season",windows.get(0).source,seasonEnd);
        if(free&&(contractEnded==null||!date.isAfter(contractEnded)))return closed("The player’s contract has not ended",windows.get(0).source,null);
        for(RegistrationWindow window:windows)if(window.includes(date))return new Decision(Status.WINDOW,"Registration window open through "+window.closes,window.source,window.closes);
        if(!free)return closed("Contracted players must join during a registration window",windows.get(0).source,null);
        if(registeredSinceRelease)return closed("The player was registered after becoming unattached; the ordinary free-agent exemption does not apply",source(country,tier),null);
        LocalDate previousClose=null;for(RegistrationWindow window:windows)if(window.closes.isBefore(date))previousClose=window.closes;
        boolean freeAtClose=previousClose!=null&&!contractEnded.isAfter(previousClose);
        LocalDate cutoff=seasonEnd;String explanation;Status status=Status.FREE_AGENT;
        switch(country) {
            case "PT":
                cutoff=LocalDate.of(year+1,2,1).with(TemporalAdjusters.lastDayOfMonth());while(cutoff.getDayOfWeek()==DayOfWeek.SATURDAY||cutoff.getDayOfWeek()==DayOfWeek.SUNDAY)cutoff=cutoff.plusDays(1);
                if(contractEnded.isAfter(LocalDate.of(year,6,30)))return closed("The ordinary Portuguese expiry exemption requires a contract ended by 30 June; other termination grounds need separate proof",source(country,tier),cutoff);
                explanation="Eligible unattached professionals can register through "+cutoff+" (last February day, with weekend adjustment).";break;
            case "DE":
                cutoff=windows.get(1).opens.minusDays(1);if(!freeAtClose||!date.isBefore(windows.get(1).opens))return closed("German ordinary free-agent registrations outside the first window end before the winter window opens",source(country,tier),cutoff);
                explanation="Unattached at the first window’s close, with no later registration; exemption ends "+cutoff+".";break;
            case "IT":
                cutoff=LocalDate.of(year+1,3,31);if(contractEnded.isAfter(LocalDate.of(year,6,30)))return closed("Italian in-season mutual releases use the ordinary windows; this exemption covers contracts ended by 30 June",source(country,tier),cutoff);
                if(date.isBefore(LocalDate.of(year,7,1)))return closed("Italian unattached-player registration opens on 1 July",source(country,tier),cutoff);
                explanation="Professionals unattached by 30 June can register from 1 July through 31 March.";break;
            case "BE":
                cutoff=LocalDate.of(year+1,3,15);if(contractEnded.isAfter(LocalDate.of(year,6,30)))return closed("This Belgian pool uses the ordinary end-of-season expiry exemption",source(country,tier),cutoff);
                if(date.isBefore(LocalDate.of(year,7,1)))return closed("Belgian end-of-season free-agent registration starts on 1 July",source(country,tier),cutoff);
                explanation="An expired professional contract permits registration from 1 July through 15 March.";break;
            case "FR":
                cutoff=windows.get(1).closes;
                if(contractEnded.isAfter(windows.get(0).closes))return closed("The French ordinary free-agent exemption requires the contract to have ended before the main registration period closed",source(country,tier),cutoff);
                explanation="Professionals released before the summer deadline, with no later registration or amateur reclassification, may register through the winter deadline.";break;
            case "SCO":
                if(!freeAtClose)return closed("The player must be unregistered at the preceding window’s close",source(country,tier),cutoff);
                explanation="Unregistered at the preceding window’s close; the ordinary Scottish exception remains available this season.";break;
            case "ES":
                if(!freeAtClose)return closed("The contract must have ended before the preceding registration period closed",source(country,tier),cutoff);
                explanation="Contracts expired before the preceding window closed may be registered outside that window.";break;
            case "ENG":
                if(!freeAtClose)return closed("The player must have been unattached before the preceding window closed",source(country,tier),cutoff);
                if(tier==2){cutoff=LocalDate.of(year+1,3,1).with(TemporalAdjusters.dayOfWeekInMonth(4,DayOfWeek.THURSDAY));explanation="EFL out-of-registration players can register through the fourth Thursday in March (17:00 in the official rule; this game advances by dates).";}
                else{status=Status.EXEMPTION_APPLICATION;explanation="Premier League rule V.4 requires Board approval outside a window. This career simulates approval for players unattached at the previous deadline, with no later registration.";}break;
            case "NL":
                if(!freeAtClose)return closed("The player was not unattached before the preceding window closed",source(country,tier),cutoff);
                status=Status.EXEMPTION_APPLICATION;explanation="KNVB transfer rules article 10 permits dispensations. This career simulates the exemption application for an unattached professional; approval is a game outcome.";break;
            case "TR":return closed("TFF article 13 uses the published registration windows. The ordinary unattached-player pool has no in-window international-certificate application",source(country,tier),null);
            default:return closed("Ordinary registration window closed; this association’s free-agent exception is not verified yet",source(country,tier),null);
        }
        if(date.isAfter(cutoff))return closed("The free-agent registration deadline was "+cutoff,source(country,tier),cutoff);
        if(year>2026)explanation+=" Later-season dates and approvals are projected from the verified rules.";
        return new Decision(status,explanation,source(country,tier),cutoff);
    }
    private static Decision closed(String explanation,String source,LocalDate cutoff){return new Decision(Status.CLOSED,explanation,source,cutoff);}
    public static String source(String country,int tier) {
        switch(country){
            case "PT":return "https://www.ligaportugal.pt/backoffice/assets/20260701_RC_2026_27_f53785bcd4.pdf";
            case "DE":return "https://media.dfl.de/sites/2/2026/04/Lizenzordnung-Spieler-LOS-2026-04-21-Stand.pdf";
            case "IT":return "https://files.figc.it/version/c%3AMjFkNjRjMzctZDUwMi00%3AOTQ1YjU5Y2QtNDhiNy00/235%20-%20Termini%20e%20disposizioni%20regolamentari%20tesseramento%20societ%C3%A0%20Serie%20A%20Serie%20B%20e%20Serie%20C%202026-2027.pdf";
            case "BE":return "https://belgianfootball.s3.eu-central-1.amazonaws.com/s3fs-public/rbfa/docs/pdf/reglement/bondsreglement_reglement_federal/URBSFA_Reglement_Livre_B_Titre_4_Joueurs.pdf";
            case "SCO":return "https://assets.ctfassets.net/qu0slcxvbwp6/2jBkzVnGXzywHTwITQoWte/e307fc2d76d249827a7e261b2797c428/ScottishFA_Handbook_2026-27_A5_DIGITAL.pdf";
            case "ES":return "https://rfef.es/sites/default/files/2025-11/20251112_Reglamento_de_Competiciones_2025.pdf";
            case "ENG":return tier==2?"https://images.gc.eflservices.co.uk/c99e4490-a070-11f1-b53b-9be1de328ad3.pdf":"https://www.sec.gov/Archives/edgar/data/1549107/000110465926110384/manu-20260630xex4d14.htm";
            case "NL":return "https://www.knvb.nl/downloads/bestand/29888/reglementen-betaald-voetbal";
            case "TR":return "https://www.tff.org/Resources/TFF/Auto/1708353ee9e34bd6846cf94461bd900c.pdf";
            case "FR":return "https://www.lfp.fr/assets/24_25_Reglement_Integral_26_07_2024_1_2c3930f8d5.pdf";
            default:return "";
        }
    }
}
