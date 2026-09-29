# Recorded crowd assets — v2.3.1

The synthetic crowd loops from v2.3 have been replaced. No Championship Manager or other game's audio is used.

| Source | Creator | Licence | Packaged use |
|---|---|---|---|
| [Crowd Ambience](https://freesound.org/people/FlatHill/sounds/324757/) | FlatHill | CC0 1.0 | Two differently sized ambience loops, short neutral reaction |
| [Millerntor Stadium Crowd Reaction Goal 01](https://freesound.org/people/itmightgetloud/sounds/829455/) | Philipp Feit / itmightgetloud | CC0 1.0 | Seven-second goal reaction excerpt |

Source pages explicitly identify Creative Commons 0, verified 29 September 2026. Public HQ MP3 previews were downloaded; authenticated original WAV downloads were not used. The goal excerpt is a crowd field recording, not a game sample. No club names or chants are added by BOSS XI.

`tools/prepare_recorded_crowd.py` records source URLs, segments and processing. It removes low-frequency rumble, crossfades loop boundaries, sets conservative RMS/peak gain and encodes 32kHz Vorbis. Longer ambience streams use asynchronously prepared MediaPlayers instead of SoundPool's short-sample decoded memory limit. Goal/reaction clips remain short mono SoundPool assets. Original ball/whistle synthesis remains at a lower mix level.

Both crowd layers pause with lifecycle/audio focus, resume only when requested, and release with the Activity. Crowd intensity follows match pressure; goal reactions duck the ambience. Reactions and kicks have wall-clock cooldowns to prevent repetitive stacking. Optional asset failure does not prevent a match.

Automated waveform/decoder checks cannot establish subjective sound quality. Physical-device listening remains required; no claim of listening to the uploaded clip is made by the automated checks.
