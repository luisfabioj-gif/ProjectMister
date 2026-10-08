package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/** All knockout cups of one country, ordered by their next actual career event. */
public final class NationalCupCampaign {
    private final List<CupSeasons> cups;
    public NationalCupCampaign(List<SeasonCup> editions) {
        if(editions==null||editions.isEmpty())throw new IllegalArgumentException("Missing national cups");
        ArrayList<CupSeasons> values=new ArrayList<>();for(SeasonCup cup:editions)values.add(new CupSeasons(cup));
        cups=Collections.unmodifiableList(values);validateRegistry();
    }
    private NationalCupCampaign(CupSeasons single){cups=Collections.singletonList(single);validateRegistry();}
    private NationalCupCampaign(Collection<CupSeasons> values){cups=Collections.unmodifiableList(new ArrayList<>(values));validateRegistry();}
    private void validateRegistry() {
        if(cups.isEmpty()||cups.size()>3)throw new IllegalArgumentException("Invalid national cup count");
        Set<String> ids=new HashSet<>();int year=cups.get(0).active().season;
        for(CupSeasons cup:cups)if(cup.active().season!=year||!ids.add(cup.active().competition))throw new IllegalArgumentException("Inconsistent national cup seasons");
    }
    public List<CupSeasons> competitions(){return cups;}
    public SeasonCup competition(String id){for(CupSeasons cup:cups)if(cup.active().competition.equals(id))return cup.active();throw new IllegalArgumentException("Unknown cup");}
    public SeasonCup active() {
        SeasonCup next=null;
        for(CupSeasons value:cups){SeasonCup cup=value.active();if(!cup.complete()&&(next==null||cup.nextDate().isBefore(next.nextDate())))next=cup;}
        return next==null?cups.get(0).active():next;
    }
    public boolean complete(){for(CupSeasons cup:cups)if(!cup.active().complete())return false;return true;}
    public List<String> archives(){ArrayList<String> result=new ArrayList<>();for(CupSeasons cup:cups)result.addAll(cup.archives());result.sort(Comparator.comparingInt(s->SeasonCup.restore(s).season));return Collections.unmodifiableList(result);}
    public NationalCupCampaign next(List<SeasonCup> editions) {
        if(!complete()||editions==null||editions.size()!=cups.size())throw new IllegalStateException("Finish national cups before annual rollover");
        ArrayList<CupSeasons> next=new ArrayList<>();
        for(CupSeasons old:cups){SeasonCup found=null;for(SeasonCup candidate:editions)if(candidate.competition.equals(old.active().competition))found=candidate;
            if(found==null)throw new IllegalArgumentException("Missing next-year cup");next.add(old.next(found));}
        return new NationalCupCampaign(next);
    }
    public NationalCupCampaign next(SeasonCup edition){return next(Collections.singletonList(edition));}
    public List<LocalDate> calendar(){ArrayList<LocalDate> result=new ArrayList<>();for(CupSeasons entry:cups){SeasonCup cup=entry.active();for(int r=0;r<cup.roundCount();r++)for(int leg=0;leg<cup.round(r).legs;leg++)result.add(cup.round(r).date(leg));}return Collections.unmodifiableList(result);}
    public void validateDate(LocalDate date){for(CupSeasons cup:cups)cup.active().validateDate(date);}
    public String snapshot(){StringBuilder out=new StringBuilder("NC1");for(CupSeasons cup:cups)out.append('\n').append(Base64.getEncoder().encodeToString(cup.snapshot().getBytes(StandardCharsets.UTF_8)));return out.toString();}
    public static NationalCupCampaign restore(String text) {
        if(text==null||text.length()>12*1024*1024)throw new IllegalArgumentException("Oversized national cup history");
        if(text.startsWith("CS1\n"))return new NationalCupCampaign(CupSeasons.restore(text));
        String[] rows=text.split("\n",-1);if(rows.length<2||rows.length>4||!rows[0].equals("NC1"))throw new IllegalArgumentException("Invalid national cup save");
        ArrayList<CupSeasons> values=new ArrayList<>();for(int i=1;i<rows.length;i++)values.add(CupSeasons.restore(new String(Base64.getDecoder().decode(rows[i]),StandardCharsets.UTF_8)));
        NationalCupCampaign campaign=new NationalCupCampaign(values);if(!text.equals(campaign.snapshot()))throw new IllegalArgumentException("Non-canonical national cup save");return campaign;
    }
    public static String name(String id) {
        switch(id){case "DE_POKAL":return "DFB-Pokal";case "ENG_FA_CUP":return "FA Cup";case "ENG_LEAGUE_CUP":return "League Cup";case "ES_COPA":return "Copa del Rey";
            case "IT_COPPA":return "Coppa Italia";case "FR_COUPE":return "Coupe de France";case "NL_BEKER":return "KNVB Cup";case "BE_CUP":return "Belgian Cup";
            case "SCO_CUP":return "Scottish Cup";case "SCO_LEAGUE_CUP":return "Scottish League Cup";case "TR_CUP":return "Türkiye Kupası";default:return id.replace('_',' ');}
    }
}
