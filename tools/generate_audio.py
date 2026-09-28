"""Original deterministic synthesis; no sampled third-party audio.
Crowd layers use distributed vocal formants, diffuse noise and spatial delays.
This is synthesized ambience, not a claim of a real stadium recording.
"""
from pathlib import Path
import numpy as np
from scipy import signal
import wave, subprocess, tempfile
SR=32000
rng=np.random.default_rng(230)
out=Path('app/src/main/res/raw');out.mkdir(parents=True,exist_ok=True)
def write(name,x,loop=False):
 x=np.asarray(x); x=x-np.mean(x,axis=0)
 peak=np.max(np.abs(x));x=x/max(peak,1e-9)*.78
 if not loop:
  fade=min(int(.012*SR),len(x)//4);x[:fade]*=np.linspace(0,1,fade)[:,None] if x.ndim==2 else np.linspace(0,1,fade);x[-fade:]*=np.linspace(1,0,fade)[:,None] if x.ndim==2 else np.linspace(1,0,fade)
 with tempfile.NamedTemporaryFile(suffix='.wav') as tmp:
  with wave.open(tmp.name,'wb') as w:
   w.setnchannels(2 if x.ndim==2 else 1);w.setsampwidth(2);w.setframerate(SR);w.writeframes((x*32767).astype('<i2').tobytes())
  subprocess.run(['ffmpeg','-v','error','-y','-i',tmp.name,'-c:a','libvorbis','-q:a','5',str(out/(name+'.ogg'))],check=True)
def crowd(seconds,energy):
 n=int(seconds*SR);t=np.arange(n)/SR;mix=np.zeros((n,2))
 for j in range(56):
  f=rng.uniform(90,235); vibr=.006*np.sin(2*np.pi*rng.uniform(3,6)*t+rng.uniform(0,6))
  voice=np.zeros(n)
  for k in range(1,20):
   freq=f*k
   weight=sum(np.exp(-((freq-form)/width)**2) for form,width in [(650,220),(1150,260),(2450,450)])/k**.7
   voice+=weight*np.sin(2*np.pi*f*k*t+vibr*k+rng.uniform(0,6))
  # irregular individual rises, not one synchronized synthetic hum
  mod=np.maximum(0,np.sin(2*np.pi*rng.uniform(.18,.65)*t+rng.uniform(0,6)))**3
  voice*=mod*energy+.10
  pan=rng.uniform(.1,.9);mix[:,0]+=voice*np.sqrt(pan);mix[:,1]+=voice*np.sqrt(1-pan)
 noise=rng.normal(size=(n,2)); sos=signal.butter(2,[160,5500],fs=SR,btype='bandpass',output='sos')
 mix+=signal.sosfilt(sos,noise,axis=0)*2.2
 for delay,gain in [(int(.089*SR),.24),(int(.173*SR),.17),(int(.291*SR),.10)]:mix[delay:]+=mix[:-delay]*gain
 # crossfade ends for seamless cyclic playback
 fade=int(.4*SR);blend=np.linspace(0,1,fade)[:,None]
 mix[:fade]=mix[-fade:]*(1-blend)+mix[:fade]*blend
 return mix[:-fade]
write('crowd_bed',crowd(6.4,.25),True)
write('crowd_pressure',crowd(5.4,.75),True)
x=crowd(3.4,1.2);env=np.minimum(1,np.arange(len(x))/(SR*.14))*np.exp(-np.arange(len(x))/(SR*2.8));write('crowd_goal',x*env[:,None])
x=crowd(1.8,.8);write('crowd_gasp',x*np.sin(np.linspace(0,np.pi,len(x)))[:,None]**2)
for name,freq,duration,power in [('ball_pass',105,.15,1),('ball_shot',76,.24,1.4),('ball_header',160,.10,.6),('ball_tackle',55,.28,.7),('ball_catch',130,.16,.7),('ball_post',880,.65,.5)]:
 t=np.arange(int(SR*duration))/SR
 x=np.sin(2*np.pi*(freq*t+freq*.009*(1-np.exp(-t*100))))*np.exp(-t*(10 if name=='ball_post' else 35))
 x+=rng.normal(size=len(t))*.34*np.exp(-t*70)
 if name=='ball_post':x+=.45*np.sin(2*np.pi*1327*t)*np.exp(-t*9)
 write(name,x*power)
t=np.arange(int(SR*.7))/SR
x=(np.sin(2*np.pi*2850*t+1.2*np.sin(2*np.pi*34*t))+.38*np.sin(2*np.pi*3650*t))*.45
x*=np.minimum(1,t/.018)*np.minimum(1,(.7-t)/.07);write('ref_whistle',x)
print('Original audio assets generated; peak target -2.2 dBFS, 32 kHz, Vorbis.')
