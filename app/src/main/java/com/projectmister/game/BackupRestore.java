package com.projectmister.game;

import android.content.SharedPreferences;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/** Validate the entire replacement before touching preferences; commit all careers together. */
public final class BackupRestore {
    public static Map<String,Object> validate(byte[] bytes)throws IOException {
        Map<String,Object> data=SaveBackup.decode(bytes);boolean career=false;
        try {
            for(int slot=0;slot<3;slot++) {
                String prefix="save_"+slot+"_";
                if(!Boolean.TRUE.equals(data.get(prefix+"exists")))continue;
                career=true;
                if(!(data.get(prefix+"club") instanceof Integer))throw new IllegalArgumentException("Missing club");
                String world=(String)data.getOrDefault(prefix+"career_world","");
                CareerDivision division=world.isEmpty()?null:CareerDivision.restore(world);
                int size=division==null?18:division.names.length,club=(Integer)data.get(prefix+"club");
                if(club<0||club>=size||division!=null&&!division.contains(club))throw new IllegalArgumentException("Invalid saved club");
                int round=(Integer)data.getOrDefault(prefix+"matchday",0);
                int maxRound=division==null?34:Math.max(division.rounds(1),division.linked?division.rounds(2):division.rounds(division.tier));
                if(round<0||round>maxRound)throw new IllegalArgumentException("Invalid matchday");
                for(String date:new String[]{"date","season_start"})if(data.containsKey(prefix+date))LocalDate.parse((String)data.get(prefix+date));
                int country=(Integer)data.getOrDefault(prefix+"manager_country_index",0);
                if(country<0||country>=8)throw new IllegalArgumentException("Invalid manager country");
                String split=(String)data.getOrDefault(prefix+"split_order","");
                if(!split.isEmpty()) {
                    if(division==null||!division.country.equals("SCO"))throw new IllegalArgumentException("Invalid split");
                    Set<Integer> seen=new HashSet<>();String[] ids=split.split(",",-1);
                    if(ids.length!=12)throw new IllegalArgumentException("Invalid split size");
                    for(String id:ids){int i=Integer.parseInt(id);if(i<0||i>=size||division.clubTiers[i]!=1||!seen.add(i))throw new IllegalArgumentException("Invalid split club");}
                } else if(division!=null&&division.country.equals("SCO")&&(division.linked||division.splitSeason())&&round>33)throw new IllegalArgumentException("Missing split");
                WorldMarket.restore((String)data.getOrDefault(prefix+"market_hires",""));
                SeasonHistory.restore((String)data.getOrDefault(prefix+"season_history",""));
                String domestic=(String)data.getOrDefault(prefix+"domestic_cup","");
                int weeks=(Integer)data.getOrDefault(prefix+"postseason_weeks",0);
                if(weeks<0||weeks>20)throw new IllegalArgumentException("Invalid postseason weeks");
                if(division!=null&&division.country.equals("PT")&&division.hasCupClubs()&&domestic.isEmpty())throw new IllegalArgumentException("Missing domestic cup");
                int cupYear=LocalDate.parse((String)data.getOrDefault(prefix+"season_start","2026-08-09")).getYear();
                String third=(String)data.getOrDefault(prefix+"portuguese_third",""),fourth=(String)data.getOrDefault(prefix+"portuguese_fourth","");
                PortugueseThirdDivisionSeasons thirdSeasons=third.isEmpty()?null:PortugueseLowerCatalog.validate(third,division,cupYear,LocalDate.parse((String)data.get(prefix+"date")));
                PortugueseFourthDivisionSeasons fourthSeasons=fourth.isEmpty()?null:PortugueseLowerCatalog.validateFourth(fourth,division,cupYear,LocalDate.parse((String)data.get(prefix+"date")));
                String district=(String)data.getOrDefault(prefix+"portuguese_district","");PortugueseDistrictSeasons districtSeasons=district.isEmpty()?null:PortugueseLowerCatalog.validateDistrict(district,division,cupYear,LocalDate.parse((String)data.get(prefix+"date")));
                if(division!=null&&division.country.equals("PT")&&division.expandedWorld()&&(thirdSeasons==null||fourthSeasons==null||districtSeasons==null))throw new IllegalArgumentException("Missing lower seasons");
                PortugueseLowerCatalog.validatePromotion((String)data.getOrDefault(prefix+"portuguese_lower_promotion",""),division,thirdSeasons,cupYear,round);
                String national=(String)data.getOrDefault(prefix+"national_cups","");
                if(division!=null&&!division.country.equals("PT")&&division.hasCupClubs()&&national.isEmpty())throw new IllegalArgumentException("Missing national cup");
                if(!national.isEmpty())NationalCupCatalog.validate(national,division,cupYear,LocalDate.parse((String)data.get(prefix+"date")));
                String scottish=(String)data.getOrDefault(prefix+"scottish_league_cup","");
                if(division!=null&&division.country.equals("SCO")&&division.hasCupClubs()&&scottish.isEmpty())throw new IllegalArgumentException("Missing Scottish League Cup");
                if(!scottish.isEmpty()) {
                    if(division==null||!division.country.equals("SCO")||!division.hasCupClubs())throw new IllegalArgumentException("Unexpected Scottish League Cup");
                    ScottishLeagueCupSeasons.validate(scottish,division.clubIds,division.reserves,cupYear,LocalDate.parse((String)data.get(prefix+"date")));
                }
                if(!domestic.isEmpty()) {
                    if(division==null||!division.country.equals("PT")||!division.hasCupClubs())throw new IllegalArgumentException("Missing cup registry");
                    DomesticCup restored=DomesticCup.restore(domestic,size);restored.validateEntrants(division.cupExclusions());
                    restored.validateDate(LocalDate.parse((String)data.get(prefix+"date")));
                    if(restored.season>cupYear||restored.season<cupYear&&!restored.complete())throw new IllegalArgumentException("Wrong domestic cup season");
                    cupYear=restored.season;
                }
                DomesticCupHistory cupHistory=DomesticCupHistory.restore((String)data.getOrDefault(prefix+"domestic_cup_history",""),size,division==null?new boolean[size]:division.cupExclusions(),cupYear);
                if(domestic.isEmpty()&&!cupHistory.editions().isEmpty())throw new IllegalArgumentException("Missing active domestic cup");
                String cup=(String)data.getOrDefault(prefix+"league_cup","");
                String modern=(String)data.getOrDefault(prefix+"modern_portuguese_cup","");
                if(!modern.isEmpty()) {
                    if(division==null||!division.linked||!division.country.equals("PT"))throw new IllegalArgumentException("Unexpected Portuguese League Cup");
                    PortugueseLeagueCupCareer.validate(modern,division.clubIds,division.clubTiers,division.reserves,LocalDate.parse((String)data.get(prefix+"season_start")).getYear(),LocalDate.parse((String)data.get(prefix+"date")));
                }
                if(!cup.isEmpty()) {
                    if(division==null||!division.linked||!division.country.equals("PT"))throw new IllegalArgumentException("Unexpected cup");
                    PortugueseLeagueCup restoredCup=PortugueseLeagueCup.restore(cup,division.clubIds);
                    restoredCup.validateDate(LocalDate.parse((String)data.get(prefix+"date")));
                }
                String history=(String)data.getOrDefault(prefix+"league_results","");
                if(!history.isEmpty())LeagueResults.restore(history,size);
                String promotion=(String)data.getOrDefault(prefix+"promotion","");
                if(!promotion.isEmpty()) {
                    PromotionCampaign p=PromotionCampaign.restore(promotion);
                    if(division==null||!division.linked||!division.country.equals(p.country())||round<maxRound)throw new IllegalArgumentException("Invalid playoff season");
                    for(int i:p.lowerEntrants())if(i>=size||division.clubTiers[i]!=2||division.reserves[i])throw new IllegalArgumentException("Invalid lower entrant");
                    for(int i:p.upperEntrants())if(i>=size||division.clubTiers[i]!=1)throw new IllegalArgumentException("Invalid upper entrant");
                }
                String teams=(String)data.getOrDefault(prefix+"p_team","");
                if(!teams.isEmpty())for(String team:teams.split(",",-1)){int i=Integer.parseInt(team);if(i<0||i>=size)throw new IllegalArgumentException("Invalid player club");}
            }
            if(!career)throw new IllegalArgumentException("No saved careers in this backup");
        } catch(Exception invalid){throw new IOException("Career data is invalid or requires a newer BOSS XI version",invalid);}
        return data;
    }
    private static boolean write(SharedPreferences prefs,Map<String,?> values) {
        SharedPreferences.Editor edit=prefs.edit().clear();
        for(Map.Entry<String,?> e:values.entrySet()) {
            Object v=e.getValue();if(v instanceof String)edit.putString(e.getKey(),(String)v);
            else if(v instanceof Integer)edit.putInt(e.getKey(),(Integer)v);
            else if(v instanceof Boolean)edit.putBoolean(e.getKey(),(Boolean)v);
            else throw new IllegalArgumentException("Unsupported preference type");
        }
        return edit.commit();
    }
    public static void restore(SharedPreferences prefs,byte[] bytes)throws IOException {
        Map<String,Object> replacement=validate(bytes);Map<String,?> previous=prefs.getAll();
        if(!write(prefs,replacement)) {
            boolean reverted=write(prefs,previous);
            throw new IOException(reverted?"Storage write failed; previous careers restored":"Storage unavailable; keep your backup and free device storage before retrying");
        }
    }
}
