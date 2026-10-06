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
captures the market/history/stadium screens. Both Android 35 (job 112353875327) and 36 (job 112353876320) passed:
https://github.com/luisfabioj-gif/ProjectMister/actions/runs/37488254392
Tested source: 1d884afda064005352c3842906c2492158c8338c.
The complete gate also passed all forty career variants, ten country playoff
flows, season transitions, backup restoration, full natural matches, compact
formations, release-split installs and different-key migration.

The first run stopped at a QA assumption that a legacy starter had an explicit
attacking slot. Its preceding recruitment/reload checks passed. The test now
sets a complete explicit XI before verifying that the overseas recruit actually
appears in a live lineup; it also checks that live matches block new signings.
No product assertion was removed. The final run above passed the corrected test.

Stadium, transfer hub, worldwide market and History hub screenshots were
reviewed. All main content/buttons were legible. A temporary rejection toast
from the scripted negative signing checks appears in the market/history
captures; this is not default page content.

Delivered file: BOSS-XI-v2.3.3-World-Market-and-History.apk, 2,341,772 bytes.
SHA-256: d53c8002f4dd411d9e2c32b32f089fc3cd33a7bd28b5617a15e439c2f376eba8.
Version code 28, version name 2.3.3-dev. Existing development signing certificate:
06d91de19adf5add10e5a54dbf14bc81d6604cdad7b783e3137a8eed55c24d0e.
APK artifact 11424497970; QA artifact 11424179602.


This candidate does not complete playable cups, authentic date calendars,
full all-country eligibility exceptions or production/physical-device QA.
