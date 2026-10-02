package com.projectmister.game;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Season-specific registration dates. Unverified/future seasons never inherit old dates silently. */
public final class RegistrationWindow {
    public final String country, label, source;
    public final LocalDate opens, closes;
    public final LocalTime deadline;
    public final ZoneId zone;
    public RegistrationWindow(String country, String label, String opens, String closes,
                              String deadline, String zone, String source) {
        this.country = country; this.label = label;
        this.opens = LocalDate.parse(opens); this.closes = LocalDate.parse(closes);
        this.deadline = deadline == null ? null : LocalTime.parse(deadline); this.zone = ZoneId.of(zone); this.source = source;
        if (this.closes.isBefore(this.opens)) throw new IllegalArgumentException("Invalid window");
    }
    /** The game advances by whole dates, including the final registration day. */
    public boolean includes(LocalDate date) { return !date.isBefore(opens) && !date.isAfter(closes); }
    public boolean includes(ZonedDateTime instant) {
        if (deadline == null) throw new IllegalStateException("Exact deadline has not been verified");
        ZonedDateTime local = instant.withZoneSameInstant(zone);
        return !local.isBefore(opens.atStartOfDay(zone)) && !local.isAfter(closes.atTime(deadline).atZone(zone));
    }
    public static List<RegistrationWindow> forCountry(String country) {
        List<RegistrationWindow> result = new ArrayList<>();
        for (RegistrationWindow w : VERIFIED_2026) if (w.country.equals(country)) result.add(w);
        return Collections.unmodifiableList(result);
    }
    /** Some associations have different opening dates by division (Italy, 2026/27). */
    public static List<RegistrationWindow> forDivision(String country, int tier) {
        if(tier < 1 || tier > 2) throw new IllegalArgumentException("Unsupported division tier");
        if(!"IT".equals(country)) return forCountry(country);
        String source="https://www.figc.it/it/federazione/news/approvati-i-criteri-per-le-riammissioni-le-sostituzioni-e-i-ripescaggi-nei-campionati-professionistici-szuz8fvh";
        List<RegistrationWindow> result=new ArrayList<>();
        result.add(new RegistrationWindow("IT", "Summer", tier==1?"2026-06-29":"2026-07-01",
                "2026-09-01", "20:00", "Europe/Rome", source));
        result.add(new RegistrationWindow("IT", "Winter", "2027-01-02", "2027-02-01",
                "20:00", "Europe/Rome", source));
        return Collections.unmodifiableList(result);
    }
    /** Explicit game calendar for later career seasons, never presented as official dates. */
    public static List<RegistrationWindow> forCareerSeason(String country,int tier,int startYear) {
        if(startYear<2026)return Collections.emptyList();
        List<RegistrationWindow> base=forDivision(country,tier);
        if(startYear==2026)return base;
        List<RegistrationWindow> result=new ArrayList<>();
        for(RegistrationWindow w:base)result.add(new RegistrationWindow(country,w.label+" (simulated)",
                w.opens.plusYears(startYear-2026).toString(),w.closes.plusYears(startYear-2026).toString(),
                w.deadline==null?null:w.deadline.toString(),w.zone.getId(),"Simulated career calendar based on 2026/27"));
        return Collections.unmodifiableList(result);
    }
    private static final List<RegistrationWindow> VERIFIED_2026 = new ArrayList<>();
    static {
        add("BE", "2026-06-17", "2026-09-03", "2027-01-02", "2027-02-03", null, "Europe/Brussels",
                "https://inside.fifa.com/associations/BEL/snapshot");
        add("SCO", "2026-06-15", "2026-09-03", "2027-01-05", "2027-02-04", null, "Europe/London",
                "https://inside.fifa.com/associations/SCO/snapshot");
        add("ES", "2026-07-01", "2026-09-01", "2027-01-02", "2027-02-01", null, "Europe/Madrid",
                "https://inside.fifa.com/associations/ESP/snapshot");
        add("DE", "2026-07-01", "2026-09-01", "2027-01-01", "2027-02-01", null, "Europe/Berlin",
                "https://inside.fifa.com/associations/GER/snapshot");
        add("NL", "2026-06-22", "2026-09-02", "2027-01-04", "2027-02-02", "23:59", "Europe/Amsterdam",
                "https://inside.fifa.com/associations/NED/snapshot");
        add("PT", "2026-07-01", "2026-09-04", "2027-01-04", "2027-02-01", "23:59", "Europe/Lisbon",
                "https://www.ligaportugal.pt/noticias/28145/prazos-de-inscricoes-na-epoca-2026-27");
        add("ENG", "2026-06-15", "2026-09-01", "2027-01-01", "2027-02-01", "23:00", "Europe/London",
                "https://www.efl.com/news/2026/may/26/transfer-window-dates-confirmed-for-season-2026-27/");
        add("FR", "2026-06-15", "2026-09-01", "2027-01-01", "2027-02-01", "19:59", "Europe/Paris",
                "https://www.lfp.fr/article/les-dates-du-mercato-2026-2027");
        add("TR", "2026-06-22", "2026-09-04", "2027-01-01", "2027-02-05", null, "Europe/Istanbul",
                "https://www.tff.org/default.aspx?ftxtID=50633&pageID=687");
    }
    private static void add(String c, String so, String sc, String wo, String wc, String time, String zone, String source) {
        VERIFIED_2026.add(new RegistrationWindow(c, "Summer", so, sc, time, zone, source));
        VERIFIED_2026.add(new RegistrationWindow(c, "Winter", wo, wc, time, zone, source));
    }
}
