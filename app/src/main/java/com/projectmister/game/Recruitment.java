package com.projectmister.game;

import java.util.*;

/** A single searchable view of active squads and deterministic world recruits. */
public final class Recruitment {
    public static final String[] SORTS={"Current ability ↓","Future ability ↓","Youngest first","Oldest first","Signing chance ↓","Transfer fee ↑","Wage ↑","Name A–Z"};
    public static final class Row {
        public final String key,name,club,country,nationality,position,status;
        public final int age,current,potential,fee,wage,chance;
        public final boolean free,owned;
        public Row(String key,String name,String club,String country,String nationality,String position,int age,int current,int potential,int fee,int wage,int chance,boolean free,boolean owned,String status){
            GameNames.protectPerson(name);this.key=key;this.name=name;this.club=GameNames.display(club);this.country=country;this.nationality=nationality;this.position=position;
            this.age=age;this.current=current;this.potential=potential;this.fee=fee;this.wage=wage;this.chance=chance;this.free=free;this.owned=owned;this.status=status;
        }
    }
    public static final class Filter {
        public String query="",position="All",country="All",club="";
        public int minAge=0,maxAge=150,minCurrent=0,minPotential=0,maxFee=9999,maxWage=9999,minChance=0;
        public boolean availableOnly;
        public boolean active(){return !query.isEmpty()||!position.equals("All")||!country.equals("All")||!club.isEmpty()||minAge!=0||maxAge!=150||minCurrent>0||minPotential>0||maxFee!=9999||maxWage!=9999||minChance>0||availableOnly;}
        public boolean accepts(Row row){
            String haystack=(row.name+" "+row.club+" "+row.country+" "+row.nationality+" "+row.position).toLowerCase(Locale.ROOT);
            for(String part:query.trim().toLowerCase(Locale.ROOT).split("\\s+"))if(!haystack.contains(part))return false;
            return (position.equals("All")||group(row.position).equals(position)||row.position.equals(position))
                &&(country.equals("All")||row.country.equals(country))&&row.club.toLowerCase(Locale.ROOT).contains(club.toLowerCase(Locale.ROOT))
                &&row.age>=minAge&&row.age<=maxAge&&row.current>=minCurrent&&row.potential>=minPotential
                &&row.fee<=maxFee&&row.wage<=maxWage&&row.chance>=minChance&&(!availableOnly||row.chance>0&&!row.owned);
        }
    }
    public static String group(String position){
        if(position.equals("GK"))return "Goalkeepers";
        if(Arrays.asList("RB","LB","CB","DC","DL","DR","LWB","RWB","SW").contains(position))return "Defenders";
        if(Arrays.asList("ST","FC","RW","LW").contains(position))return "Forwards";
        return "Midfielders";
    }
    public static List<Row> select(Collection<Row> rows,Filter filter,int sort){
        List<Row> out=new ArrayList<>();for(Row row:rows)if(filter.accepts(row))out.add(row);
        Comparator<Row> order;
        switch(sort){
            case 1:order=Comparator.comparingInt((Row r)->r.potential).reversed();break;
            case 2:order=Comparator.comparingInt(r->r.age);break;
            case 3:order=Comparator.comparingInt((Row r)->r.age).reversed();break;
            case 4:order=Comparator.comparingInt((Row r)->r.chance).reversed();break;
            case 5:order=Comparator.comparingInt(r->r.fee);break;
            case 6:order=Comparator.comparingInt(r->r.wage);break;
            case 7:order=Comparator.comparing(r->r.name,String.CASE_INSENSITIVE_ORDER);break;
            default:order=Comparator.comparingInt((Row r)->r.current).reversed();
        }
        out.sort(order.thenComparing(r->r.name,String.CASE_INSENSITIVE_ORDER).thenComparing(r->r.key));return out;
    }
    private Recruitment(){}
}
