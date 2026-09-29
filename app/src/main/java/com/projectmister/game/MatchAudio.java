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
    private final MediaPlayer[] ambience=new MediaPlayer[2];
    private final boolean[] prepared=new boolean[2];
    private boolean active,closed,crowd=true,effects=true,focused,wantsPlayback;
    private float pressure=.2f,level=.15f,peak,duck;
    private long lastKick,lastReaction;
    private final Runnable tick=new Runnable(){ public void run(){
        if(closed||!active)return;
        level+=(pressure-level)*.12f;peak*=.91f;duck*=.76f;
        float volume=crowd?Math.max(0,.34f-level*.10f-peak*.12f-duck):0;
        setAmbienceVolume(0,volume);
        float swell=crowd?Math.max(0,level*.30f+peak*.12f-duck):0;
        setAmbienceVolume(1,swell);
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
        loadAmbience(context,0,R.raw.crowd_bed,attrs);loadAmbience(context,1,R.raw.crowd_pressure,attrs);
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
    private void loadAmbience(Context c,int index,int res,AudioAttributes attrs){
        MediaPlayer player=new MediaPlayer();ambience[index]=player;
        try(android.content.res.AssetFileDescriptor asset=c.getResources().openRawResourceFd(res)){
            player.setAudioAttributes(attrs);player.setDataSource(asset.getFileDescriptor(),asset.getStartOffset(),asset.getLength());
            player.setLooping(true);player.setVolume(0,0);
            player.setOnPreparedListener(p->{if(closed)return;prepared[index]=true;if(active)p.start();});
            player.setOnErrorListener((p,what,extra)->{prepared[index]=false;return true;});player.prepareAsync();
        }catch(Exception e){player.release();ambience[index]=null;android.util.Log.w("BOSSXI","Optional crowd recording unavailable",e);}
    }
    private void setAmbienceVolume(int index,float volume){
        if(prepared[index]&&ambience[index]!=null)try{ambience[index].setVolume(volume,volume);}catch(IllegalStateException ignored){}
    }
    private void startLoops(){
        for(int i=0;i<2;i++)if(prepared[i]&&ambience[i]!=null)try{ambience[i].start();}catch(IllegalStateException ignored){}
    }
    void pressure(float value,boolean late){pressure=MatchMath.clamp(value+(late?.1f:0),0,1);}
    void cue(String cue){
        if(closed||!active)return;
        boolean reaction=cue.equals("goal")||cue.equals("save")||cue.equals("miss");
        if(reaction&&!crowd || !reaction&&!effects)return;
        if(cue.equals("miss"))cue="save";
        long now=SystemClock.uptimeMillis();
        if(cue.equals("kick")){if(now-lastKick<240)return;lastKick=now;}
        if(reaction&&!cue.equals("goal")){if(now-lastReaction<2000)return;lastReaction=now;}
        if(cue.equals("goal"))peak=1;
        if(cue.equals("whistle"))duck=.25f;
        Integer id=sounds.get(cue);if(id==null||!loaded.contains(id))return;
        float volume=cue.equals("goal")?.82f:cue.equals("whistle")?.38f:reaction?.32f:.16f+random.nextFloat()*.06f;
        float pan=.85f+random.nextFloat()*.15f;
        pool.play(id,volume,volume*pan,2,0,cue.equals("whistle")?1:.96f+random.nextFloat()*.08f);
    }
    private void suspendPlayback(){if(closed)return;active=false;handler.removeCallbacks(tick);pool.autoPause();for(int i=0;i<2;i++)if(prepared[i]&&ambience[i]!=null)try{ambience[i].pause();}catch(IllegalStateException ignored){} }
    void pause(){wantsPlayback=false;suspendPlayback();}
    void close(){if(closed)return;pause();closed=true;handler.removeCallbacksAndMessages(null);pool.release();for(MediaPlayer p:ambience)if(p!=null)p.release();manager.abandonAudioFocusRequest(focus);loaded.clear();}
}
