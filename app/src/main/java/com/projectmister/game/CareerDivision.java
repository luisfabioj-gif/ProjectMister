package com.projectmister.game;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;

/** A save owns its club order: future catalog changes cannot remap player/team indices. */
public final class CareerDivision {
    public final String id, country, name;
    public final int tier;
    public final String[] clubIds, names;
    public final int[] strengths, budgets, primary, secondary;
    public final boolean[] reserves;
    public final int[] clubTiers;
    public final String[] tierNames;
    public final boolean linked;
    public boolean hasCupClubs(){for(int t:clubTiers)if(t==3)return true;return false;}
    public CareerDivision withCupClubs(DomesticCupCatalog cup) {
        if(!linked||!country.equals("PT")||hasCupClubs())throw new IllegalArgumentException("Invalid cup expansion");
        try {
            JSONObject o=new JSONObject(snapshot());JSONArray existing=o.getJSONArray("clubs");
            for(int i=0;i<cup.clubs.length();i++) {
                JSONObject c=cup.clubs.getJSONObject(i);String identity=c.getString("id");int level=c.getInt("level"),seed=identity.hashCode()&0x7fffffff;
                existing.put(new JSONObject().put("id",identity).put("name",c.getString("name")).put("tier",3).put("reserve",false)
                    .put("strength",(level==3?53:level==4?45:38)+seed%12).put("budget",1+seed%3).put("primary",0xff28674f).put("secondary",0xfff4f5f7));
            }
            o.put("schema",3);return new CareerDivision(o);
        }catch(Exception e){throw new IllegalArgumentException("Cannot expand Portuguese cup world",e);}
    }
    public static CareerDivision countryCareer(CompetitionCatalog catalog,CompetitionCatalog.Division active) {
        try {
            JSONObject o=new JSONObject(new CareerDivision(active).snapshot());
            JSONArray clubs=o.getJSONArray("clubs");String[] labels=new String[2];labels[active.tier-1]=active.name;
            for(int i=0;i<clubs.length();i++)clubs.getJSONObject(i).put("tier",active.tier);
            for(CompetitionCatalog.Division other:catalog.divisions)if(other.countryCode.equals(active.countryCode)&&other.tier!=active.tier) {
                labels[other.tier-1]=other.name;
                JSONArray added=new JSONObject(new CareerDivision(other).snapshot()).getJSONArray("clubs");
                for(int i=0;i<added.length();i++)clubs.put(added.getJSONObject(i).put("tier",other.tier));
            }
            if(labels[0]==null||labels[1]==null)throw new IllegalArgumentException("Country requires two divisions");
            o.put("schema",2).put("tierNames",new JSONArray(labels));return new CareerDivision(o);
        }catch(Exception error){throw new IllegalArgumentException("Cannot create linked career",error);}
    }
    public int[] members(int level) {
        int count=0;for(int t:clubTiers)if(t==level)count++;
        int[] ids=new int[count];int i=0;for(int club=0;club<clubTiers.length;club++)if(clubTiers[club]==level)ids[i++]=club;
        return ids;
    }
    public boolean contains(int club){return club>=0&&club<clubTiers.length&&clubTiers[club]==tier;}
    public String nameFor(int level){return tierNames[level-1];}
    public int rounds(int level) {
        int count=members(level).length;if(count==0)return 0;
        return country.equals("SCO")&&level==1?38:(count+count%2-1)*(country.equals("SCO")?4:2);
    }
    /** Membership changes never reorder identities or regenerate a player's ID. */
    public CareerDivision moveBetweenTiers(int[] up,int[] down,int managerClub) {
        if(!linked||up.length!=down.length)throw new IllegalArgumentException("Unbalanced season transition");
        try {
            JSONObject o=new JSONObject(snapshot());JSONArray clubs=o.getJSONArray("clubs");HashSet<Integer> seen=new HashSet<>();
            for(int id:up){if(id<0||id>=names.length||clubTiers[id]!=2||reserves[id]||!seen.add(id))throw new IllegalArgumentException("Ineligible promotion");clubs.getJSONObject(id).put("tier",1);}
            for(int id:down){if(id<0||id>=names.length||clubTiers[id]!=1||!seen.add(id))throw new IllegalArgumentException("Invalid relegation");clubs.getJSONObject(id).put("tier",2);}
            int next=clubs.getJSONObject(managerClub).getInt("tier");
            o.put("tier",next).put("name",tierNames[next-1]).put("id",id.substring(0,id.lastIndexOf(':')+1)+next);
            return new CareerDivision(o);
        }catch(Exception error){throw new IllegalArgumentException("Invalid season transition",error);}
    }
    public CareerDivision(CompetitionCatalog.Division division) {
        id=division.id;country=division.countryCode;name=division.name;tier=division.tier;
        linked=false;tierNames=new String[]{tier==1?name:"",tier==2?name:""};
        int n=division.clubs.size();clubTiers=new int[n];java.util.Arrays.fill(clubTiers,tier);clubIds=new String[n];names=new String[n];reserves=new boolean[n];
        strengths=new int[n];budgets=new int[n];primary=new int[n];secondary=new int[n];
        int[] palette={0xffbb3038,0xff285da8,0xff237c51,0xffa87922,0xff667188,0xff763d83};
        for(int i=0;i<n;i++) {
            CompetitionCatalog.Club club=division.clubs.get(i);clubIds[i]=club.id;names[i]=club.name;reserves[i]=club.reserve;
            int seed=club.id.hashCode()&0x7fffffff;
            // Explicitly fictional simulation values, not claimed real finances or licensed ratings.
            strengths[i]=(tier==1?69:57)+seed%15;budgets[i]=(tier==1?8:2)+seed%(tier==1?25:7);
            primary[i]=palette[seed%palette.length];secondary[i]=0xfff4f5f7;
        }
    }
    private CareerDivision(JSONObject o)throws Exception {
        int schema=o.getInt("schema");if(schema<1||schema>3)throw new IllegalArgumentException("Unsupported career database");
        linked=schema>=2;
        id=o.getString("id");country=o.getString("country");name=o.getString("name");tier=o.getInt("tier");
        if(tier<1||tier>2)throw new IllegalArgumentException("Invalid tier");
        JSONArray a=o.getJSONArray("clubs");int n=a.length();
        if(n<10||n>(schema==3?256:linked?48:24)||schema==3&&!country.equals("PT"))throw new IllegalArgumentException("Invalid club count");
        clubTiers=new int[n];tierNames=new String[]{tier==1?name:"",tier==2?name:""};
        if(linked){JSONArray labels=o.getJSONArray("tierNames");if(labels.length()!=2)throw new IllegalArgumentException("Invalid tier names");for(int i=0;i<2;i++)tierNames[i]=labels.getString(i);}
        clubIds=new String[n];names=new String[n];reserves=new boolean[n];strengths=new int[n];budgets=new int[n];primary=new int[n];secondary=new int[n];
        HashSet<String> seen=new HashSet<>();
        for(int i=0;i<n;i++) {
            JSONObject c=a.getJSONObject(i);clubTiers[i]=c.optInt("tier",tier);if(clubTiers[i]<1||clubTiers[i]>(schema==3?3:2))throw new IllegalArgumentException("Invalid membership");clubIds[i]=c.getString("id");names[i]=c.getString("name");
            if(!seen.add(clubIds[i])||names[i].isEmpty())throw new IllegalArgumentException("Invalid club identity");
            reserves[i]=c.optBoolean("reserve",false);strengths[i]=c.getInt("strength");budgets[i]=c.getInt("budget");
            primary[i]=c.getInt("primary");secondary[i]=c.getInt("secondary");
        }
        if(linked)for(int level=1;level<=2;level++){int size=members(level).length;if(size<10||size>24||tierNames[level-1].isEmpty())throw new IllegalArgumentException("Invalid linked division");}
    }
    public static CareerDivision restore(String value)throws Exception {return new CareerDivision(new JSONObject(value));}
    public String snapshot() {
        try {
            JSONObject o=new JSONObject().put("schema",hasCupClubs()?3:linked?2:1).put("id",id).put("country",country).put("name",name).put("tier",tier);
            if(linked)o.put("tierNames",new JSONArray(tierNames));
            JSONArray a=new JSONArray();for(int i=0;i<names.length;i++)a.put(new JSONObject().put("id",clubIds[i]).put("name",names[i])
                    .put("tier",clubTiers[i]).put("reserve",reserves[i]).put("strength",strengths[i]).put("budget",budgets[i]).put("primary",primary[i]).put("secondary",secondary[i]));
            return o.put("clubs",a).toString();
        }catch(Exception e){throw new IllegalStateException("Cannot encode career database",e);}
    }
    public boolean splitSeason(){return country.equals("SCO")&&tier==1;}
    public int meetings(){return country.equals("SCO")?(tier==1?3:4):2;}
    public int rounds(){return rounds(tier);}
}
