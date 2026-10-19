package com.projectmister.game;

/** Clock and actions remain suspended until the celebration and physical reset finish. */
final class MatchRestart {
    enum Phase { LIVE, CELEBRATING, RESETTING }
    private Phase phase=Phase.LIVE;
    private float elapsed,settled;
    void goal(){phase=Phase.CELEBRATING;elapsed=settled=0;}
    void kickoff(){phase=Phase.RESETTING;elapsed=settled=0;}
    boolean active(){return phase!=Phase.LIVE;}
    boolean celebrating(){return phase==Phase.CELEBRATING;}
    boolean resetting(){return phase==Phase.RESETTING;}
    void step(float dt,boolean playersReady){
        elapsed+=Math.max(0,dt);
        if(celebrating()&&elapsed>=3.4f){kickoff();return;}
        if(resetting()){
            settled=playersReady?settled+Math.max(0,dt):0;
            if(settled>=.25f)phase=Phase.LIVE;
        }
    }
    static float x(float anchor,boolean home,int slot,boolean takingKickoff){
        if(slot==9&&takingKickoff)return home?.493f:.507f;
        float own=.04f+(home?anchor:1-anchor)*.54f;return home?Math.min(.47f,own):Math.max(.53f,1-own);
    }
    static float y(float anchor,int slot,boolean takingKickoff){return slot==9&&takingKickoff?.5f:anchor;}
}
