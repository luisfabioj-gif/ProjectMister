// The runner deliberately uses reflection instead of production QA hooks.
// javac cannot catch a misspelled literal Activity method, so check those reads
// against the committed production source before building the runner.
const fs = require('fs');
const main = fs.readFileSync('app/src/main/java/com/projectmister/game/MainActivity.java', 'utf8');
const qa = fs.readFileSync('app/src/androidTest/java/com/projectmister/game/qa/SmokeRunner.java', 'utf8');
const names = [...new Set([...qa.matchAll(/call\("(\w+)"/g)].map(match => match[1]))];
const missing = names.filter(name => !new RegExp('(?:private|public|protected)\\s+(?:static\\s+)?[\\w.<>\\[\\]]+\\s+' + name + '\\s*\\(').test(main));
if (missing.length) {
  process.stderr.write('Missing reflective Activity methods: ' + missing.join(', ') + '\n');
  process.exit(1);
}
process.stdout.write('PASS: ' + names.length + ' literal QA method names exist in the production Activity\n');
