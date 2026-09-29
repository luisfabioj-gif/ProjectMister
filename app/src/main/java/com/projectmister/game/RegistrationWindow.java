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
    private static final List<RegistrationWindow> VERIFIED_2026 = new ArrayList<>();
    static {
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
