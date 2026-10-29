package com.projectmister.game;

/** Time-bounded goal emphasis, independent of simulation speed and later commentary. */
public final class GoalFlash {
    public static final long DURATION=3600,INTERVAL=300;
    public int team=-1;
    public String line="";
    private long started=-1;
    public void start(int team,String line,long now){this.team=team;this.line=line;started=now;}
    public boolean active(long now){return started>=0&&now>=started&&now-started<DURATION;}
    public int phase(long now){return active(now)?(int)((now-started)/INTERVAL%2):-1;}
    public void clear(){started=-1;team=-1;line="";}
}
