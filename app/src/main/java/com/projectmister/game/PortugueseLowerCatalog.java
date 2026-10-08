package com.projectmister.game;
import java.util.*;
import org.json.*;

/** Reserve identities excluded from the national cup, plus published 2026/27 lower fields. */
public final class PortugueseLowerCatalog {
    private static final String[] NORTH={"vianense","pacos-de-ferreira","ad-fafe","varzim","trofense","paredes","marco-09","sao-joao-de-ver","leca","vitoria-sc-b"};
    private static final String[][] FOURTH={
        {"braganca","brito","desp-celoricense","gd-chaves-b","limianos","maia-lidador","maria-da-fonte","montalegre","ponte-da-barca","rebordosa","sc-braga-b","tirsense","vila-mea","vinhais"},
        {"alpendorada","beira-mar","camacha","castro-daire","cinfaes","estrela-da-calheta","florgrade","guarda-fc","machico","ovarense","salgueiros","sanjoanense","sousense","u-lamas"},
        {"ad-nogueirense","at-malveira","benfica-castelo-branco","fatima","fazendense","marialvas","mortagua","naval-1893","nazarenos","o-elvas","oliveira-do-hospital","sertanense","uniao-da-serra","fc-alverca-b"},
        {"1-o-dezembro","alcochetense","aljustrelense","amora","imortal","juv-lajense","juventude-de-evora","lagoa","portel","real","serpa","sintrense","santa-clara-b","v-setubal"}};
    public static JSONArray reserves() {
        JSONArray out=new JSONArray();String[] ids={"vitoria-sc-b","sc-braga-b","gd-chaves-b","fc-alverca-b","santa-clara-b"},names={"Vitória SC B","SC Braga B","GD Chaves B","FC Alverca B","Santa Clara B"};
        try{for(int i=0;i<ids.length;i++)out.put(new JSONObject().put("id","pt:cup-"+ids[i]).put("name",names[i]).put("level",i==0?3:4).put("reserve",true));}catch(Exception e){throw new IllegalStateException(e);}return out;
    }
    private static int region(String id) {
        for(String club:NORTH)if(id.equals("pt:cup-"+club))return 0;
        for(int g=0;g<4;g++)for(String club:FOURTH[g])if(id.equals("pt:cup-"+club))return g;
        String parent=ReserveEligibility.parent(id);if(!parent.isEmpty())id=parent;
        return Arrays.asList("pt:fc-porto","pt:sc-braga","pt:vitoria-sc","pt:fc-famalicao","pt:moreirense-fc","pt:rio-ave-fc","pt:gil-vicente-fc","pt:fc-arouca","pt:afs","pt:amarante-fc","pt:cd-feirense","pt:cd-tondela","pt:fc-felgueiras","pt:fc-penafiel","pt:fc-vizela","pt:gd-chaves","pt:lusitania-lourosa-fc","pt:leixoes-sc","pt:academico-de-viseu").contains(id)?0:3;
    }
    public static int[][] groups(CareerDivision world,int level,int count,int year) {
        ArrayList<Integer> clubs=new ArrayList<>();for(int c=0;c<world.names.length;c++)if(world.association(c).equals("PT")&&world.level(c)==level)clubs.add(c);
        if(clubs.size()%count!=0||level==3&&clubs.size()!=20||level==4&&clubs.size()!=56&&clubs.size()!=64)throw new IllegalArgumentException("Incomplete Portuguese lower field");
        clubs.sort((a,b)->{int r=Integer.compare(region(world.clubIds[a]),region(world.clubIds[b]));return r!=0?r:world.clubIds[a].compareTo(world.clubIds[b]);});
        int[][] groups=new int[count][clubs.size()/count];for(int i=0;i<clubs.size();i++)groups[i/groups[0].length][i%groups[0].length]=clubs.get(i);
        if(year==2026&&level==4)for(int g=0;g<4;g++)for(int i=0;i<14;i++){int found=-1;for(int c:clubs)if(world.clubIds[c].equals("pt:cup-"+FOURTH[g][i]))found=c;if(found<0)throw new IllegalArgumentException("Missing Campeonato de Portugal club");groups[g][i]=found;}
        return groups;
    }
    public static PortugueseThirdDivisionSeason create(CareerDivision world,int year,long seed){int[][] groups=groups(world,3,2,year);return new PortugueseThirdDivisionSeason(year,seed,world.names.length,groups[0],groups[1]);}
    public static PortugueseThirdDivisionSeasons validate(String text,CareerDivision world,int year,java.time.LocalDate date) {
        PortugueseThirdDivisionSeasons seasons=PortugueseThirdDivisionSeasons.restore(text);PortugueseThirdDivisionSeason active=seasons.active();
        if(world==null||!world.country.equals("PT")||active.season!=year||active.worldSize!=world.names.length)throw new IllegalArgumentException("Changed Liga 3 world");
        Set<Integer> expected=new HashSet<>();for(int c=0;c<world.names.length;c++)if(world.association(c).equals("PT")&&world.level(c)==3)expected.add(c);
        Set<Integer> actual=new HashSet<>();for(int c:active.clubs())actual.add(c);if(!actual.equals(expected))throw new IllegalArgumentException("Changed Liga 3 field");active.validateDate(date);
        for(String archive:seasons.archives())for(int c:PortugueseThirdDivisionSeason.restore(archive).clubs())if(!world.association(c).equals("PT"))throw new IllegalArgumentException("Foreign Liga 3 club");return seasons;
    }
    public static PortugueseFourthDivisionSeasons validateFourth(String text,CareerDivision world,int year,java.time.LocalDate date) {
        PortugueseFourthDivisionSeasons seasons=PortugueseFourthDivisionSeasons.restore(text);PortugueseFourthDivisionSeason active=seasons.active();
        if(world==null||!world.country.equals("PT")||active.season!=year||active.worldSize!=world.names.length)throw new IllegalArgumentException("Changed lower qualifying world");Set<Integer> expected=new HashSet<>();for(int c=0;c<world.names.length;c++)if(world.association(c).equals("PT")&&world.level(c)==4)expected.add(c);Set<Integer> actual=new HashSet<>();for(int c:active.clubs())actual.add(c);if(!actual.equals(expected))throw new IllegalArgumentException("Changed lower qualifying field");active.validateDate(date);for(String archive:seasons.archives())for(int c:PortugueseFourthDivisionSeason.restore(archive).clubs())if(!world.association(c).equals("PT"))throw new IllegalArgumentException("Foreign lower qualifying club");return seasons;
    }
    public static PortugueseDistrictSeason createDistrict(CareerDivision world,int year,long seed){ArrayList<Integer> clubs=new ArrayList<>();for(int c=0;c<world.names.length;c++)if(world.association(c).equals("PT")&&world.level(c)==5)clubs.add(c);clubs.sort((a,b)->{int r=Integer.compare(region(world.clubIds[a]),region(world.clubIds[b]));return r!=0?r:world.clubIds[a].compareTo(world.clubIds[b]);});int[][] groups=new int[4][];int cursor=0;for(int g=0;g<4;g++){groups[g]=new int[clubs.size()/4+(g<clubs.size()%4?1:0)];for(int i=0;i<groups[g].length;i++)groups[g][i]=clubs.get(cursor++);}return new PortugueseDistrictSeason(year,seed,world.names.length,groups);}
    public static PortugueseDistrictSeasons validateDistrict(String text,CareerDivision world,int year,java.time.LocalDate date){PortugueseDistrictSeasons editions=PortugueseDistrictSeasons.restore(text);PortugueseDistrictSeason active=editions.active();if(world==null||!world.country.equals("PT")||active.season!=year||active.worldSize!=world.names.length)throw new IllegalArgumentException("Changed district world");Set<Integer> expected=new HashSet<>(),actual=new HashSet<>();for(int c=0;c<world.names.length;c++)if(world.association(c).equals("PT")&&world.level(c)==5)expected.add(c);for(int c:active.clubs())actual.add(c);if(!expected.equals(actual))throw new IllegalArgumentException("Changed district qualifying field");active.validateDate(date);for(String archive:editions.archives())for(int c:PortugueseDistrictSeason.restore(archive).clubs())if(!world.association(c).equals("PT"))throw new IllegalArgumentException("Foreign district qualifier");return editions;}
    public static PortugueseLowerPromotion validatePromotion(String text,CareerDivision world,PortugueseThirdDivisionSeasons third,int year,int round) {
        if(text==null||text.isEmpty())return null;PortugueseLowerPromotion p=PortugueseLowerPromotion.restore(text);if(world==null||!world.country.equals("PT")||third==null||!third.active().complete()||p.season!=year||round<Math.max(world.rounds(1),world.rounds(2)))throw new IllegalArgumentException("Unexpected lower barrage");
        if(!java.util.Arrays.equals(p.thirdEntrants(),java.util.Arrays.copyOf(third.active().promotionOrder(),3)))throw new IllegalArgumentException("Changed Liga 3 final promotion order");
        for(int c:p.thirdEntrants())if(c>=world.names.length||!world.association(c).equals("PT")||world.level(c)!=3)throw new IllegalArgumentException("Invalid Liga 3 barrage entrant");for(int c:p.secondEntrants())if(c>=world.names.length||world.clubTiers[c]!=2)throw new IllegalArgumentException("Invalid Liga 2 barrage entrant");return p;
    }
    private PortugueseLowerCatalog(){}
}
