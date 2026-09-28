package com.projectmister.game;

import android.content.Context;
import android.media.*;
import android.os.*;
import java.util.*;

/** Event-driven bounded mixer; optional audio can never prevent gameplay. */
final class MatchAudio {
    private final SoundPool pool;
    private final Map<String,Integer> sounds=new HashMap<>();
    private final Set<Integer> loaded=new HashSet<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Random random=new Random();
    private final AudioManager manager;
    private final AudioFocusRequest focus;
    private int bedStream,pressureStream;
    private boolean active,closed,crowd=true,effects=true,focused,wantsPlayback;
    private float pressure=.2f,level=.15f,peak,duck;
    private long lastKick;
    private final Runnable tick=new Runnable(){ public void run(){
        if(closed||!active)return;
        level+=(pressure-level)*.12f;peak*=.91f;duck*=.76f;
        float volume=crowd?Math.max(0,.20f+level*.18f+peak*.10f-duck):0;
        pool.setVolume(bedStream,volume,volume);
        float swell=crowd?Math.max(0,level*.32f+peak*.28f-duck):0;
        pool.setVolume(pressureStream,swell,swell);
        handler.postDelayed(this,80);
    }};
    MatchAudio(Context context){
        AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attrs).setOnAudioFocusChangeListener(change->{
            if(closed)return;
            if(change==AudioManager.AUDIOFOCUS_LOSS || change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
                focused=false;
                if(change==AudioManager.AUDIOFOCUS_LOSS)wantsPlayback=false;
                suspendPlayback();
            }
            else if(change==AudioManager.AUDIOFOCUS_GAIN){focused=true;if(wantsPlayback)resumePlayback();}
            else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)duck=.3f;
        }).build();
        pool=new SoundPool.Builder().setMaxStreams(10).setAudioAttributes(attrs).build();
        pool.setOnLoadCompleteListener((p,id,status)->{if(status==0&&!closed){loaded.add(id);if(active)startLoops();}});
        load(context,"bed",R.raw.crowd_bed);load(context,"pressure",R.raw.crowd_pressure);
        load(context,"goal",R.raw.crowd_goal);load(context,"save",R.raw.crowd_gasp);
        load(context,"kick",R.raw.ball_pass);load(context,"shot",R.raw.ball_shot);
        load(context,"header",R.raw.ball_header);load(context,"tackle",R.raw.ball_tackle);
        load(context,"catch",R.raw.ball_catch);load(context,"post",R.raw.ball_post);
        load(context,"whistle",R.raw.ref_whistle);
    }
    private void load(Context c,String name,int res){try{sounds.put(name,pool.load(c,res,1));}catch(RuntimeException e){android.util.Log.w("BOSSXI","Optional sound unavailable",e);}}
    void settings(boolean crowd,boolean effects){this.crowd=crowd;this.effects=effects;}
    void start(){
        if(closed)return;
        wantsPlayback=true;
        if(active)return;
        focused=manager.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if(!focused)return;
        resumePlayback();
    }
    private void resumePlayback(){
        if(closed||active||!wantsPlayback||!focused)return;
        active=true;pool.autoResume();startLoops();handler.removeCallbacks(tick);handler.post(tick);
    }
    private void startLoops(){
        Integer bed=sounds.get("bed"), swell=sounds.get("pressure");
        // Keep the two loops alive when several short event sounds overlap.
        if(bedStream==0&&bed!=null&&loaded.contains(bed))bedStream=pool.play(bed,0,0,3,-1,1);
        if(pressureStream==0&&swell!=null&&loaded.contains(swell))pressureStream=pool.play(swell,0,0,3,-1,1);
    }
    void pressure(float value,boolean late){pressure=MatchMath.clamp(value+(late?.1f:0),0,1);}
    void cue(String cue){
        if(closed||!active)return;
        boolean reaction=cue.equals("goal")||cue.equals("save")||cue.equals("miss");
        if(reaction&&!crowd || !reaction&&!effects)return;
        if(cue.equals("miss"))cue="save";
        long now=SystemClock.uptimeMillis();
        if(cue.equals("kick")){if(now-lastKick<120)return;lastKick=now;}
        if(cue.equals("goal"))peak=1;
        if(cue.equals("whistle"))duck=.25f;
        Integer id=sounds.get(cue);if(id==null||!loaded.contains(id))return;
        float volume=cue.equals("goal")?.82f:cue.equals("whistle")?.38f:reaction?.50f:.30f+random.nextFloat()*.12f;
        float pan=.85f+random.nextFloat()*.15f;
        pool.play(id,volume,volume*pan,2,0,cue.equals("whistle")?1:.96f+random.nextFloat()*.08f);
    }
    private void suspendPlayback(){if(closed)return;active=false;handler.removeCallbacks(tick);pool.autoPause();}
    void pause(){wantsPlayback=false;suspendPlayback();}
    void close(){if(closed)return;pause();closed=true;handler.removeCallbacksAndMessages(null);pool.release();manager.abandonAudioFocusRequest(focus);loaded.clear();}
}
