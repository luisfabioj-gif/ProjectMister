# BOSS XI 2.3.3 development candidate

## Requested changes

- History button combines the saved-career archive with an offline reference
  collection: selected historical winners/facts for ten top flights, three UEFA
  competitions and the Portuguese cup/league cup. Each entry links official
  sources; verified 6 October 2026. Coverage is explicitly selective, not a
  complete all-time database. Real history never awards simulated trophies.
- Worldwide transfer browser exposes all twenty catalog divisions. Clubs in
  the active world use their existing players and negotiation flow. Other clubs
  offer deterministic fictional recruits through confirmed fee/contract terms.
  Foreign divisions are recruitment sources, not concurrently simulated leagues.
- Sixty fictional unattached candidates have zero transfer fees. Fees, wage
  capacity, duplicate recruitment, local-club duplication and the current
  registration window are checked at confirmation. National out-of-window
  free-agent exemptions remain unimplemented; the UI states the window policy.
- Up to 512 external signings per career are appended with stable contiguous
  player IDs. Recruitment identities, roster attributes, tactics and financial
  consequences survive reload/backup. Old saves default to an empty registry.
  Recruitment does not reorder league clubs or existing players. Imported
  portraits retain their source identity after signing.
- Original Portuguese club defaults now use real names, with abbreviations.
  Existing deliberate editor overrides are preserved. The classic save keeps
  its original membership; new database careers use the 2026/27 catalog.
- Transfer hub displays the managed league's configured registration windows,
  replacing its previously hardcoded Portuguese dates.
- Passing leads moving receivers without changing the target mid-flight.
  Close opponents steer apart while remaining within tackling distance.
- Added original illustrative stadium and construction images and 36 fictional
  photographic male player portraits, bringing the total portrait library to 78
  (66 players, six male staff and six female manager portraits). Existing saved
  portrait assignments remain unchanged.

## Assets and prompts

Built-in image generation was used for all new artwork, not photographs of
real players or real venues. Stadium assets are downscaled WebP images; the
6-by-6 portrait atlas is cut into 36 individually loaded 224px WebP tiles.
Original source images remain in the task workspace; runtime assets are in
app/src/main/assets/stadium and app/src/main/assets/portraits/person_42–77.webp.

Prompt set: (1) photorealistic fictional medium-sized European stadium, roofed
stands, teal seats, dusk floodlights, elevated corner, accurate pitch, no text
or branding; (2) single 6-by-6 aligned headshot atlas of 36 distinct fictional
adult male footballers, varied ethnic backgrounds/hairstyles, charcoal studio
background, plain teal shirts, realistic skin, no labels/brands/celebrities;
(3) photorealistic stadium stand development, steel roof structure and cranes,
completed teal seating alongside construction, daylight, no text or logos.

## Validation

Pure Java regressions include recruit determinism, free fees, malformed and
duplicate identities, backup fields and bounded pass leading. Application and
independent instrumentation runner compile with the direct SDK build.
Android QA adds signings sourced from all twenty divisions in a Portuguese
legacy career, a free-agent signing, rejection for duplicate/fee/wage/window
failures, reload/backup checks and an imported player in a live lineup. It also
captures the market/history/stadium screens. Full Android 35/36 CI is pending.

This candidate does not complete playable cups, authentic date calendars,
full all-country eligibility exceptions or production/physical-device QA.
