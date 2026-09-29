"""Prepare CC0 field recordings; requires numpy and ffmpeg. No game audio is used.
Public HQ previews are used under each recording's CC0 licence. See docs/AUDIO_SOURCES.md.
"""
from pathlib import Path
import subprocess,urllib.request,hashlib
import numpy as np
root=Path(__file__).resolve().parents[1]
out=root/'app/src/main/res/raw'
cache=root/'app/build/audio-source';cache.mkdir(parents=True,exist_ok=True)
sources={
 'crowd':('https://cdn.freesound.org/previews/324/324757_3839718-hq.mp3','/tmp/crowd-recording.mp3'),
 'goal':('https://cdn.freesound.org/previews/829/829455_3625328-hq.mp3','/tmp/goal-recording.mp3')}
sr=32000
samples={}
for name,(url,local) in sources.items():
 p=cache/(name+'.mp3')
 if not p.exists():
  if Path(local).exists():p.write_bytes(Path(local).read_bytes())
  else:
   with urllib.request.urlopen(url,timeout=60) as r:p.write_bytes(r.read())
 print(name,hashlib.sha256(p.read_bytes()).hexdigest())
 data=subprocess.check_output(['ffmpeg','-v','error','-i',str(p),'-ac','2','-ar',str(sr),'-af','highpass=f=100,lowpass=f=10500','-f','f32le','-'])
 samples[name]=np.frombuffer(data,dtype='<f4').reshape(-1,2).copy()
 # Show energy only to identify recording segments; this is not a listening test.
 print('RMS per second:',[round(float(np.sqrt(np.mean(samples[name][i*sr:(i+1)*sr]**2))),3) for i in range(min(15,len(samples[name])//sr))])
def write(name,source,start,seconds,loop=False,rms=.10,mono=False):
 a=samples[source][int(start*sr):int((start+seconds)*sr)].copy()
 if loop:
  n=int(.7*sr);f=np.linspace(0,1,n)[:,None]
  seam=a[-n:]*(1-f)+a[:n]*f
  a=np.concatenate([seam,a[n:-n]])
 else:
  n=int(.03*sr);a[:n]*=np.linspace(0,1,n)[:,None]
  n=int(.5*sr);a[-n:]*=np.linspace(1,0,n)[:,None]
 a*=min(rms/max(1e-6,float(np.sqrt(np.mean(a*a)))),.72/max(1e-6,float(np.max(np.abs(a)))))
 if mono:a=a.mean(axis=1)
 subprocess.run(['ffmpeg','-v','error','-y','-f','f32le','-ar',str(sr),'-ac','1' if mono else '2','-i','-','-c:a','libvorbis','-q:a','5',str(out/(name+'.ogg'))],input=a.astype('<f4').tobytes(),check=True)
 print(name,'seconds',round(len(a)/sr,2),'peak',round(float(np.max(np.abs(a))),3))
write('crowd_bed','crowd',2,25,True,.075)
write('crowd_pressure','crowd',32,20,True,.105)
write('crowd_goal','goal',1,7,False,.16,True)
# A short neutral swell, without pretending every miss is a goal roar.
write('crowd_gasp','crowd',47,2.2,False,.09,True)
