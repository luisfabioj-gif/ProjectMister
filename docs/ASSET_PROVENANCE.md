# v2.3 asset provenance

Portraits: generated with the built-in image generation tool for this project, fictional people only, without supplied real-person references. Two atlases inspected and cropped into 42 square 256×256 WebP portraits, quality 88. Files: app/src/main/assets/portraits/person_00.webp through person_41.webp. First 30 are young/senior players; 30–35 male staff; 36–41 female managers. Deliberate reuse across the database. Persisted identity assignment; no network or per-launch generation.

Prompt set: (1) precise 6×6 grid of 36 fictional male football professionals, first 18 ages 18–24, next 12 ages 27–38, last six coaches ages 40–65; diverse appearances; front-facing head-and-shoulders professional studio photography, realistic skin/hair/eyes, navy logo-free sports clothing, charcoal background, no real people, text or borders. (2) precise 3×2 grid of six fictional female managers, ages 30–60, diverse appearances, same photographic framing and lighting, navy clothing, no identifiable real people or logos. Generated via built-in ImageGen. Packaged crops are versioned here.

New audio: original deterministic synthesis in tools/generate_audio.py, NumPy/SciPy plus ffmpeg Vorbis encoding. No third-party recordings or game samples. Peak target -2.2dBFS; 32kHz; loops six/five seconds within Android SoundPool decoded-size limits. Synthesized vocal/formant ambience, not a real stadium recording. Hardware listening QA remains necessary.

Existing pitch/logo and legacy WAV files are inherited from the user's Hotfix 4 baseline. This document makes no new provenance assertion for historical assets.

New pitch presentation: original Java Canvas drawing in PitchArt.java, shared by live match and tactics. It uses geometric field markings and restrained green mowing stripes, with no third-party pitch artwork.
