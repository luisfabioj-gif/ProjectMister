package com.projectmister.game;

import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Submitted simulated UEFA lists. Ordinary 2026/27 rules are projected into
 * later career editions; these lists never assert real club registrations. */
public final class UEFARegistration {
    public static final int LEAGUE=10,KNOCKOUT=11;
    public static final class Profile {
        public final int id,club,rating,training;
        public final boolean goalkeeper,listB,baseline;
        public final LocalDate joined,qualified;
        public Profile(int id,int club,int rating,boolean keeper,boolean clubTrained,boolean associationTrained,boolean listB,LocalDate joined,LocalDate qualified,boolean baseline) {
            if(id<0||id>=CareerLimits.MAX_CLUBS*64||club<0||club>=CareerLimits.MAX_CLUBS||rating<0||rating>100||joined==null||qualified==null||qualified.isBefore(joined))throw new IllegalArgumentException("Invalid UEFA player profile");
            this.id=id;this.club=club;this.rating=rating;goalkeeper=keeper;training=clubTrained?2:associationTrained?1:0;this.listB=listB;this.joined=joined;this.qualified=qualified;this.baseline=baseline;
        }
    }
    public static final class Entry {
        public final int id,training;
        public final boolean goalkeeper,keeperExemption;
        Entry(Profile p){this(p.id,p.training,p.goalkeeper,false);}
        Entry(int id,int training,boolean keeper,boolean exemption){this.id=id;this.training=training;goalkeeper=keeper;keeperExemption=exemption;}
    }
    public static final class Book {
        public final int club,stage;
        public final String competition;
        public final LocalDate firstLeg,deadline,closes;
        private LocalDate submitted,bSubmitted;
        private boolean manual,frozen;
        private final TreeMap<Integer,Entry> a=new TreeMap<>(),b=new TreeMap<>();
        private final TreeSet<Integer> base=new TreeSet<>(),late=new TreeSet<>(),winter=new TreeSet<>(),replacements=new TreeSet<>();
        Book(int year,int club,String competition,int stage,LocalDate firstLeg){this.club=club;this.competition=competition;this.stage=stage;this.firstLeg=firstLeg;deadline=deadline(year,competition,stage);closes=stage<LEAGUE?firstLeg.minusDays(1):deadline;}
        public List<Entry> listA(){return Collections.unmodifiableList(new ArrayList<>(a.values()));}
        public List<Entry> listB(){return Collections.unmodifiableList(new ArrayList<>(b.values()));}
        public boolean listAContains(int id){return a.containsKey(id);}
        public boolean contains(int id){return a.containsKey(id)||b.containsKey(id);}
        public boolean winterRegistered(int id){return stage==KNOCKOUT&&a.containsKey(id)&&winter.contains(id);}
        public boolean frozen(){return frozen;}
        public int lateAdditions(){return late.size();}
        public int winterAdditions(){return winter.size();}
        public int capacity(){return UEFARegistration.capacity(a.values());}
        public int clubTrained(){int n=0;for(Entry e:a.values())if(e.training==2)n++;return n;}
        public int associationTrained(){int n=0;for(Entry e:a.values())if(e.training==1)n++;return n;}
        public int goalkeepersA(){return keepers(a.values());}
        public int goalkeepersTotal(){return goalkeepersA()+keepers(b.values());}
        public LocalDate submittedOn(){return submitted;}
    }
    public final int year;
    private final TreeMap<String,Book> books=new TreeMap<>();
    private final ArrayList<String> archives=new ArrayList<>();
    public UEFARegistration(int year){if(year<2026||year>2200)throw new IllegalArgumentException("Invalid UEFA registration edition");this.year=year;}
    public static int stage(int phase){return phase>=KNOCKOUT?KNOCKOUT:phase;}
    public static LocalDate deadline(int year,int stage){switch(stage){case 1:return LocalDate.of(year,7,3);case 2:return LocalDate.of(year,7,17);case 3:return LocalDate.of(year,7,31);case 4:return LocalDate.of(year,8,14);case LEAGUE:return LocalDate.of(year,9,2);case KNOCKOUT:return LocalDate.of(year+1,2,4);default:throw new IllegalArgumentException("Invalid UEFA list stage");}}
    public static LocalDate deadline(int year,String competition,int stage){if(competition==null||!competition.matches("UEFA_(CHAMPIONS|EUROPA|CONFERENCE)"))throw new IllegalArgumentException("Invalid UEFA list competition");LocalDate date=deadline(year,stage);return stage<LEAGUE&&competition.equals("UEFA_CHAMPIONS")?date.minusDays(1):date;}
    private static String key(int club,String competition,int stage){return club+":"+competition+":"+stage;}
    public Book book(int club,String competition,int phase){return books.get(key(club,competition,stage(phase)));}
    public List<Book> books(){return Collections.unmodifiableList(new ArrayList<>(books.values()));}
    public List<String> archives(){return Collections.unmodifiableList(archives);}
    private static int keepers(Collection<Entry> entries){int n=0;for(Entry e:entries)if(e.goalkeeper)n++;return n;}
    private static int capacity(Collection<Entry> entries){int ct=0,at=0;for(Entry e:entries){if(e.training==2)ct++;else if(e.training==1)at++;}return 17+Math.min(8,ct+Math.min(4,at));}
    private static String sheetIssue(Collection<Entry> a,Collection<Entry> b){if(a.size()>25||a.size()>capacity(a))return "List A exceeds its homegrown capacity";if(keepers(a)<2||keepers(a)+keepers(b)<3)return "Register two goalkeepers on List A and three across A/B";return "";}
    private TreeMap<Integer,Profile> roster(int club,Collection<Profile> profiles){TreeMap<Integer,Profile> out=new TreeMap<>();for(Profile p:profiles)if(p.club!=club||out.put(p.id,p)!=null)throw new IllegalArgumentException("Changed UEFA squad");return out;}
    private static boolean eligibleBy(Profile p,LocalDate date){return p.baseline||!p.joined.isAfter(date)&&!p.qualified.isAfter(date);}
    private static ArrayList<Profile> ranked(Collection<Profile> profiles){ArrayList<Profile> out=new ArrayList<>(profiles);out.sort((a,b)->a.rating==b.rating?Integer.compare(a.id,b.id):Integer.compare(b.rating,a.rating));return out;}
    private static TreeMap<Integer,Entry> select(Collection<Profile> profiles,Set<Integer> prior) {
        ArrayList<Profile> ranked=ranked(profiles);TreeMap<Integer,Entry> out=new TreeMap<>();
        for(boolean retained:new boolean[]{true,false})for(Profile p:ranked)if(p.goalkeeper&&prior.contains(p.id)==retained&&keepers(out.values())<2)out.put(p.id,new Entry(p));
        for(int training=2;training>=0;training--)for(Profile p:ranked)if(p.training==training&&!p.listB&&!out.containsKey(p.id)&&out.size()<25){out.put(p.id,new Entry(p));if(out.size()>capacity(out.values()))out.remove(p.id);}
        return out;
    }
    private static TreeMap<Integer,Entry> bList(Collection<Profile> profiles,Map<Integer,Entry> a,LocalDate by){TreeMap<Integer,Entry> b=new TreeMap<>();for(Profile p:profiles)if(p.listB&&eligibleBy(p,by)&&!a.containsKey(p.id))b.put(p.id,new Entry(p));return b;}
    /** Called at the chronological match preparation, or while editing a draft.
     * First access reconstructs only registrations whose stored dates permit it. */
    public Book prepare(int club,String competition,int phase,LocalDate firstLeg,LocalDate matchDate,LocalDate now,int leagueRound,Collection<Profile> profiles,Set<Integer> permanentlyDepartedKeepers) {
        int stage=stage(phase);LocalDate limit=deadline(year,competition,stage);
        if(club<0||club>=CareerLimits.MAX_CLUBS||competition==null||!competition.matches("UEFA_(CHAMPIONS|EUROPA|CONFERENCE)")||now==null||matchDate==null||firstLeg==null||firstLeg.isBefore(LocalDate.of(year,7,1))||firstLeg.isAfter(LocalDate.of(year+1,7,31))||stage<LEAGUE&&!firstLeg.isAfter(limit))throw new IllegalArgumentException("Invalid UEFA registration fixture");
        TreeMap<Integer,Profile> roster=roster(club,profiles);Book book=book(club,competition,stage);
        if(book==null){book=new Book(year,club,competition,stage,firstLeg);books.put(key(club,competition,stage),book);}
        else if(stage<LEAGUE&&!book.firstLeg.equals(firstLeg))throw new IllegalArgumentException("Changed qualifying first-leg deadline");
        if(!book.frozen&&!book.manual)auto(book,roster,now);
        if(!now.isBefore(book.deadline)&&stage<LEAGUE&&book.base.isEmpty())book.base.addAll(book.a.keySet());
        if(stage==LEAGUE&&!competition.equals("UEFA_CONFERENCE")&&leagueRound>=6&&permanentlyDepartedKeepers!=null)replaceDepartedKeepers(book,roster,matchDate.minusDays(1),permanentlyDepartedKeepers);
        LocalDate by=now.isBefore(matchDate.minusDays(1))?now:matchDate.minusDays(1);book.b.clear();book.b.putAll(bList(roster.values(),book.a,by));book.bSubmitted=by;
        if(now.isAfter(book.closes))book.frozen=true;
        return book;
    }
    private void auto(Book book,TreeMap<Integer,Profile> roster,LocalDate now) {
        LocalDate by=now.isBefore(book.deadline)?now:book.deadline;
        if(book.stage==KNOCKOUT) {
            Book original=book(book.club,book.competition,LEAGUE);if(original==null)throw new IllegalStateException("Submit the league-phase list before the knockout list");
            book.base.clear();book.base.addAll(original.base);if(book.base.isEmpty())book.base.addAll(original.a.keySet());
            ArrayList<Profile> retained=new ArrayList<>(),fresh=new ArrayList<>();for(Profile p:roster.values())if(eligibleBy(p,by)){if(book.base.contains(p.id)&&!p.joined.isAfter(original.deadline))retained.add(p);else if(!p.listB||p.goalkeeper)fresh.add(p);}
            int keeperCount=0;for(Profile p:retained)if(p.goalkeeper)keeperCount++;for(Profile p:fresh)if(p.goalkeeper&&original.replacements.contains(p.id))keeperCount++;final boolean needKeeper=keeperCount<2;
            fresh.removeIf(p->p.listB&&p.goalkeeper&&!needKeeper&&!original.replacements.contains(p.id));
            fresh.sort((a,b)->{int ap=original.replacements.contains(a.id)?0:needKeeper&&a.goalkeeper?1:2,bp=original.replacements.contains(b.id)?0:needKeeper&&b.goalkeeper?1:2;return ap!=bp?Integer.compare(ap,bp):Integer.compare(b.rating,a.rating);});
            ArrayList<Profile> allowed=new ArrayList<>(retained);for(int i=0;i<Math.min(3,fresh.size());i++)allowed.add(fresh.get(i));
            book.a.clear();book.a.putAll(select(allowed,original.a.keySet()));book.winter.clear();for(int id:book.a.keySet())if(!book.base.contains(id)||roster.get(id).joined.isAfter(original.deadline))book.winter.add(id);
        } else {
            ArrayList<Profile> initial=new ArrayList<>();for(Profile p:roster.values())if(eligibleBy(p,by))initial.add(p);
            if(book.stage==LEAGUE||now.isBefore(book.deadline)||book.base.isEmpty()){book.a.clear();book.a.putAll(select(initial,Collections.emptySet()));book.base.clear();book.base.addAll(book.a.keySet());book.late.clear();}
            if(book.stage<LEAGUE&&!now.isBefore(book.deadline)) {
                TreeMap<Integer,Entry> selected=new TreeMap<>();for(int id:book.base)if(roster.containsKey(id))selected.put(id,new Entry(roster.get(id)));
                ArrayList<Profile> late=new ArrayList<>();LocalDate cutoff=now.isBefore(book.closes)?now:book.closes;for(Profile p:roster.values())if(!book.base.contains(p.id)&&!p.listB&&eligibleBy(p,cutoff))late.add(p);
                for(Profile p:ranked(late))if(book.late.contains(p.id)||book.late.size()<2){selected.put(p.id,new Entry(p));if(selected.size()>25||selected.size()>capacity(selected.values())){selected.remove(p.id);continue;}book.late.add(p.id);}
                book.a.clear();book.a.putAll(selected);
            }
        }
        book.submitted=now.isBefore(book.closes)?now:book.closes;
    }
    private void replaceDepartedKeepers(Book book,TreeMap<Integer,Profile> roster,LocalDate by,Set<Integer> departed) {
        for(int old:new ArrayList<>(book.a.keySet()))if(book.a.get(old).goalkeeper&&departed.contains(old)) {
            Entry previous=book.a.get(old);ArrayList<Profile> candidates=ranked(roster.values());candidates.sort((a,b)->a.listB!=b.listB?a.listB?1:-1:Integer.compare(b.rating,a.rating));for(Profile p:candidates)if(p.goalkeeper&&!book.a.containsKey(p.id)&&eligibleBy(p,by)) {
                book.a.remove(old);book.a.put(p.id,new Entry(p.id,previous.training,true,true));book.replacements.add(p.id);break;
            }
        }
    }
    /** A failed manual submission leaves every saved list/counter unchanged. */
    public String submit(Book book,Collection<Integer> ids,Collection<Profile> profiles,LocalDate now) {
        if(book==null||book!=this.book(book.club,book.competition,book.stage)||now==null)return "No current UEFA list";
        if(now.isAfter(book.closes))return "List A deadline has passed ("+book.closes+")";
        TreeMap<Integer,Profile> roster=roster(book.club,profiles);TreeMap<Integer,Entry> selected=new TreeMap<>();
        for(int id:ids){Profile p=roster.get(id);if(p==null||!eligibleBy(p,now)||selected.put(id,new Entry(p))!=null)return "Choose distinct, currently qualified squad players";}
        TreeSet<Integer> late=new TreeSet<>(book.late),winter=new TreeSet<>(book.winter);
        if(book.stage<LEAGUE&&now.isAfter(book.deadline))for(int id:selected.keySet())if(!book.base.contains(id))late.add(id);
        if(book.stage==KNOCKOUT)for(int id:selected.keySet())if(!book.base.contains(id)||roster.get(id).joined.isAfter(deadline(year,LEAGUE)))winter.add(id);
        if(late.size()>2)return "Only two new List A players may be added after this qualifying deadline";
        if(winter.size()>3)return "Only three new List A players may be registered for the knockout phase";
        TreeMap<Integer,Entry> b=bList(roster.values(),selected,now);String issue=sheetIssue(selected.values(),b.values());if(!issue.isEmpty())return issue;
        book.a.clear();book.a.putAll(selected);book.b.clear();book.b.putAll(b);book.bSubmitted=now;book.manual=true;book.submitted=now;book.late.clear();book.late.addAll(late);book.winter.clear();book.winter.addAll(winter);
        if(book.stage!=KNOCKOUT&&!now.isAfter(book.deadline)){book.base.clear();book.base.addAll(selected.keySet());}return "";
    }
    public String reason(int club,String competition,int phase,int id) {
        Book book=book(club,competition,phase);if(book==null)return "UEFA player list has not been submitted";
        String issue=sheetIssue(book.a.values(),book.b.values());if(!issue.isEmpty())return issue;
        return book.contains(id)?"":"Not named on this stage's UEFA List A or B";
    }
    public UEFARegistration nextSeason(int nextYear){if(nextYear!=year+1)throw new IllegalArgumentException("Missing UEFA list edition");UEFARegistration next=new UEFARegistration(nextYear);next.archives.addAll(archives);next.archives.add(activeSnapshot());if(next.archives.size()>5)next.archives.remove(0);return next;}
    private static String ids(Collection<Integer> ids){StringJoiner out=new StringJoiner(",");for(int id:ids)out.add(Integer.toString(id));return out.length()==0?"-":out.toString();}
    private static TreeSet<Integer> ids(String text){TreeSet<Integer> out=new TreeSet<>();if(!text.equals("-"))for(String s:text.split(",",-1)){int id=Integer.parseInt(s);if(id<0||id>=CareerLimits.MAX_CLUBS*64||!out.add(id))throw new IllegalArgumentException("Invalid UEFA list player");}return out;}
    private String activeSnapshot(){StringBuilder out=new StringBuilder("UR1|").append(year);for(Book b:books.values()){out.append("\nK|").append(b.club).append('|').append(b.competition).append('|').append(b.stage).append('|').append(b.firstLeg).append('|').append(b.submitted==null?"-":b.submitted).append('|').append(b.manual?1:0).append('|').append(b.frozen?1:0).append('|').append(b.bSubmitted==null?"-":b.bSubmitted).append('|').append(ids(b.base)).append('|').append(ids(b.late)).append('|').append(ids(b.winter)).append('|').append(ids(b.replacements));for(Entry e:b.a.values())out.append("\nA|").append(e.id).append('|').append(e.goalkeeper?1:0).append('|').append(e.training).append('|').append(e.keeperExemption?1:0);for(Entry e:b.b.values())out.append("\nB|").append(e.id).append('|').append(e.goalkeeper?1:0);}return out.toString();}
    public String snapshot(){StringBuilder out=new StringBuilder(activeSnapshot());for(String archive:archives)out.append("\nH|").append(Base64.getEncoder().encodeToString(archive.getBytes(StandardCharsets.UTF_8)));return out.toString();}
    public static UEFARegistration restore(String text) {
        if(text==null||text.length()>24*1024*1024)throw new IllegalArgumentException("Oversized UEFA lists");String[] rows=text.split("\n",-1),header=rows[0].split("\\|",-1);if(header.length!=2||!header[0].equals("UR1")||rows.length>500000)throw new IllegalArgumentException("Invalid UEFA list schema");UEFARegistration out=new UEFARegistration(Integer.parseInt(header[1]));Book book=null;
        for(int i=1;i<rows.length;i++){String[] r=rows[i].split("\\|",-1);
            if(r[0].equals("K")&&r.length==13){int club=Integer.parseInt(r[1]),stage=Integer.parseInt(r[3]);LocalDate first=LocalDate.parse(r[4]);if(club<0||club>=CareerLimits.MAX_CLUBS||!r[2].matches("UEFA_(CHAMPIONS|EUROPA|CONFERENCE)")||out.books.size()>=CareerLimits.MAX_CLUBS*18)throw new IllegalArgumentException("Invalid UEFA list club");book=new Book(out.year,club,r[2],stage,first);book.submitted=r[5].equals("-")?null:LocalDate.parse(r[5]);book.manual=r[6].equals("1");book.frozen=r[7].equals("1");book.bSubmitted=r[8].equals("-")?null:LocalDate.parse(r[8]);book.base.addAll(ids(r[9]));book.late.addAll(ids(r[10]));book.winter.addAll(ids(r[11]));book.replacements.addAll(ids(r[12]));if(!r[6].matches("[01]")||!r[7].matches("[01]")||stage<KNOCKOUT&&!book.winter.isEmpty()||stage>=LEAGUE&&!book.late.isEmpty()||book.late.size()>2||book.winter.size()>3||book.base.size()>25||out.books.put(key(club,r[2],stage),book)!=null)throw new IllegalArgumentException("Invalid UEFA additions");}
            else if(r[0].equals("A")&&r.length==5&&book!=null){int id=Integer.parseInt(r[1]),training=Integer.parseInt(r[3]);if(id<0||id>=CareerLimits.MAX_CLUBS*64||training<0||training>2||!r[2].matches("[01]")||!r[4].matches("[01]")||r[4].equals("1")&&!r[2].equals("1")||book.a.size()>=25||book.a.put(id,new Entry(id,training,r[2].equals("1"),r[4].equals("1")))!=null)throw new IllegalArgumentException("Invalid UEFA List A");}
            else if(r[0].equals("B")&&r.length==3&&book!=null){int id=Integer.parseInt(r[1]);if(id<0||id>=CareerLimits.MAX_CLUBS*64||!r[2].matches("[01]")||book.a.containsKey(id)||book.b.put(id,new Entry(id,0,r[2].equals("1"),false))!=null)throw new IllegalArgumentException("Invalid UEFA List B");}
            else if(r[0].equals("H")&&r.length==2&&out.archives.size()<5){String archive=new String(Base64.getDecoder().decode(r[1]),StandardCharsets.UTF_8);if(archive.contains("\nH|"))throw new IllegalArgumentException("Nested UEFA list archive");UEFARegistration previous=restore(archive);if(previous.year>=out.year||!out.archives.isEmpty()&&restore(out.archives.get(out.archives.size()-1)).year+1!=previous.year)throw new IllegalArgumentException("Invalid UEFA list archive year");out.archives.add(archive);book=null;}
            else throw new IllegalArgumentException("Invalid UEFA list row");
        }
        if(!text.equals(out.snapshot()))throw new IllegalArgumentException("Noncanonical UEFA lists");return out;
    }
    public void validate(int edition,int playerCount,int clubCount,LocalDate current) {
        if(year!=edition||playerCount<1||playerCount>CareerLimits.MAX_CLUBS*64||clubCount<2||clubCount>CareerLimits.MAX_CLUBS||current==null)throw new IllegalArgumentException("Changed UEFA registration edition");
        for(Book b:books.values()){if(b.club>=clubCount||b.firstLeg.isBefore(LocalDate.of(year,7,1))||b.firstLeg.isAfter(LocalDate.of(year+1,7,31))||b.stage<LEAGUE&&!b.firstLeg.isAfter(b.deadline)||b.submitted==null||b.submitted.isAfter(current)||b.submitted.isAfter(b.closes)||b.submitted.isBefore(LocalDate.of(year,7,1))||b.bSubmitted==null||b.bSubmitted.isAfter(current)||b.bSubmitted.isBefore(LocalDate.of(year,7,1)))throw new IllegalArgumentException("Invalid saved UEFA submission date");
            TreeSet<Integer> all=new TreeSet<>(b.a.keySet());all.addAll(b.b.keySet());all.addAll(b.base);all.addAll(b.late);all.addAll(b.winter);all.addAll(b.replacements);if(!all.isEmpty()&&all.last()>=playerCount||b.a.size()>capacity(b.a.values())||b.replacements.size()>25)throw new IllegalArgumentException("Invalid saved UEFA roster");
            for(Entry e:b.a.values())if(e.keeperExemption&&!b.replacements.contains(e.id))throw new IllegalArgumentException("Unrecorded goalkeeper replacement");
        }
        for(String archive:archives){UEFARegistration past=restore(archive);past.validate(past.year,playerCount,clubCount,LocalDate.of(past.year+1,7,31));}
    }
    public void validateGoalkeepers(Set<Integer> goalkeepers){if(goalkeepers==null)throw new IllegalArgumentException("Missing goalkeeper registry");for(Book book:books.values()){ArrayList<Entry> entries=new ArrayList<>(book.a.values());entries.addAll(book.b.values());for(Entry e:entries)if(e.goalkeeper!=goalkeepers.contains(e.id))throw new IllegalArgumentException("UEFA keeper flag disagrees with player identity");}for(String archive:archives)restore(archive).validateGoalkeepers(goalkeepers);}
}
