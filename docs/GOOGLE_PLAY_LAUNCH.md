# BOSS XI — Google Play launch guide

Updated 8 November 2026. This is an internal-test preparation kit. The final
native regression run for the latest text/editor fixes and real Play payment
testing are still pending. See `QA-STATUS.md` in the kit before uploading.

This launch kit contains BOSS XI 2.3.18, build 44, for Android 8 and later.
Google Play package: `com.projectmister.game`. The game is free to install and
play. The Game Editor is an optional, permanent purchase for changing club names
and colours across careers.

The unsigned AAB in the kit is the release candidate. Complete the steps below
to produce the signed AAB that Play Console accepts. The test APK is for local
play and career checks. Purchases stay locked in that APK.

## 1. Create or open your Play Console app

Sign in at [Play Console](https://play.google.com/console/). If you are new,
[register as a developer](https://support.google.com/googleplay/android-developer/answer/6112435)
and complete the identity and Android device verification shown on your
dashboard. Registration has a one-time US$25 fee.

Choose **Create app**: BOSS XI, English (United Kingdom) if offered, **Game**,
**Free**. If you already created a Play Console entry for this package, use it
and check its upload certificate and highest version code before signing.
Choose **Sports** as the game category. Select the countries where you want to
launch. Check the app's package registration status under Android developer
verification in Play Console. Set up or verify the
[Google payments profile](https://support.google.com/googleplay/android-developer/answer/3092739)
and payout details to receive editor sales.

## 2. Get the public key and set up the editor product

Go to **Monetize → Monetization setup → Licensing** and copy the app's
**Base64 public RSA licence key**. The app uses this public key to check
receipts. It is separate from the private upload signing key.

In **Monetize with Play → Products → One-time products**, create the following
product. If product creation is disabled until a bundle is uploaded, finish
step 3 first, then return here before testing payments.

| Field | Value |
|---|---|
| Product ID | `boss_xi_game_editor` |
| Name | BOSS XI Game Editor |
| Description | Permanently unlock the Game Editor to rename clubs and change team colours across your careers. |
| Purchase option ID | `editor-unlock` |
| Purchase type | **Buy** |
| Quantity | Single purchase; leave multi-quantity off |
| Price | Your choice; **£3.99** is a suggested UK starting price |

Set the price and availability in your chosen markets and activate the Buy
option. Use the two IDs exactly as written; keep this product a single purchase
with no rent, pre-order, or discount options for the first release. The app
reads the price and currency directly from Google Play, so price changes do
not require an app update.

If the licence key itself is unavailable before the first upload, use an
internal draft only to register the package and retrieve the key. A configured
build then needs a higher version code if build 44 has already been used.

[Google's product setup guide](https://support.google.com/googleplay/android-developer/answer/16430488)
and [where to find the licence key](https://support.google.com/googleplay/android-developer/answer/186113).

## 3. Publish your policy and prepare the AAB

Fill in the publisher name, support email and effective date in
`privacy-policy.html`. Host it on a public HTTPS page that anyone can read
without signing in. Check the URL in a private browser window and use it in
Play Console. The app also has a privacy screen in Career Hub and Club Hub.

On a computer with a full JDK 17 or later, open a terminal in the extracted
launch kit. If this is the first Play upload for BOSS XI, create a new upload
key once. Keep its password and a backup of the file somewhere private:

```text
keytool -genkeypair -storetype PKCS12 -keystore boss-xi-upload.p12 -alias boss-xi-upload -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=BOSS XI Upload"
```

If this package already has an upload key registered in Play Console, use that
key and its alias. Sign using the kit's tool:

```text
java PreparePlayUpload.java BOSS-XI-v2.3.18-UNSIGNED.aab BOSS-XI-v2.3.18-Upload.aab boss-xi-upload.p12 boss-xi-upload
```

Enter the public licence key, hosted privacy URL and support email when asked.
Then enter your private keystore password at the signing prompt. The tool
replaces only the public release settings in the verified bundle, checks the
signature and refuses the public BOSS XI development key. The resulting
`BOSS-XI-v2.3.18-Upload.aab` is the file to upload. You can also build the
source with those public settings and sign the AAB in Android Studio.

In Play Console choose **Test and release → Testing → Internal testing →
Create new release**. Enrol in **Play App Signing** if requested; for a first
release, Google can generate and hold the app signing key. Upload the signed
AAB, add the release notes from `store-listing.txt`, and roll it out to the
internal track. The upload key signs the file you send to Google. Google uses
its app signing key for the copies installed by players.

[Google's app signing instructions](https://developer.android.com/studio/publish/app-signing).

## 4. Complete the store listing and App content

- In **Grow users → Store presence → Main store listing**, paste the title and
  descriptions from `store-listing.txt`, add your support email, and upload
  the 512 × 512 icon, 1,024 × 500 feature graphic and phone screenshots from
  `graphics/`.
- In **Policy and programs → App content**, add the hosted privacy policy,
  finish the Data safety and IARC content rating forms, and set the target
  audience to match how you market the game. Answer **No ads** for this build.
  Core play is available without a game login. In App access, disclose the
  optional paid editor and provide reviewer instructions for **Career Hub →
  Game Editor**. Declare the
  optional in-game purchase where asked. The purchase contains no random items.
- For Data safety, use the release facts below and Google's definitions. The
  app has no developer backend, user account, ads or developer analytics.
  Manager name and birth date, saves, settings and team edits are stored in
  private app storage. Android may back them up according to device settings;
  the user can separately choose a storage provider for a manual export. The
  editor uses the Google Play Billing SDK, which may process purchase and
  diagnostic information. The game does not see card details or send receipt
  data to the publisher. Review billing SDK disclosures for the version in the
  uploaded bundle and reflect its processing in the form. Do not answer from
  older BOSS XI builds, which had no billing.
- Review rights to the shipped content before production. The game uses original
  shield artwork, alternate club identities, generic playable competition and
  stadium labels, generated portraits, and original or documented CC0 audio.
  Real-world football history remains as reference. Altered names do not by
  themselves establish permission to use a name or mark. The listing does not
  claim association with any league or club.

[Store graphic requirements](https://support.google.com/googleplay/android-developer/answer/9866151),
[App content](https://support.google.com/googleplay/android-developer/answer/9859455),
[Data safety](https://support.google.com/googleplay/android-developer/answer/10787469)
and [Google Play's intellectual property policy](https://support.google.com/googleplay/android-developer/answer/9888072).

## 5. Test payments and saves through Play

Add a test Google account both to the internal track and to **Settings →
License testing**. Install the app from the internal test link, with that
account active in Google Play. Use the Play test card options to check:

1. A new player can create a career and play a match; Game Editor shows the
   local price, with no edit or reset controls before purchase.
2. Cancelling, a declined test card, and a pending test payment keep the
   editor locked. An approved test purchase opens it and allows a club edit.
3. Restart the app. Reinstall using the same Google account and use
   **Restore purchase**. The editor is available without another charge.
   Try a different account and refund/revoke a test order, then check that
   the editor is locked again on the next successful Play check.
4. Open a game, play to half-time and full-time, and check audio, substitutions,
   saves and the inbox on your phone. Review the Play pre-launch report.

If you already have a sideloaded career: **Career Hub → Back up & restore careers
→ Export backup**. Keep the file in Downloads or another location outside the
app, then uninstall the test APK, install the Play version, and restore the
file. Android will not update the public test APK in place with a Play-signed
copy. A manual backup is not encrypted. It never grants the editor entitlement.

[Google's billing test instructions](https://developer.android.com/google/play/billing/test).

## 6. Apply for production and release

If your personal developer account was created after 13 November 2023,
[Google requires](https://support.google.com/googleplay/android-developer/answer/14151465)
a closed test with at least 12 testers opted in continuously for 14 days before
you can apply for production access. Internal testers do not count towards that
closed test. Run the closed track, collect feedback, and apply from the app
dashboard. Once Google grants access and the payment and policy checks are
complete, promote the tested bundle to Production and submit it for review.

Keep the original kit, your upload key and the configured AAB. Increase the
version code for each future upload. Nothing in this kit publishes the game or
sets a live price by itself.
