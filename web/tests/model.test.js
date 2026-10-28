import test from 'node:test';
import assert from 'node:assert/strict';
import { Library, SAMPLES, csvField, toCsv } from '../model.js';

test('starts with nine unassigned samples and consistent counts', () => {
  const library = new Library();
  assert.equal(library.counts().total, 9); assert.equal(library.counts().unassigned, 9); assert.equal(library.counts().sorted, 0);
});
test('undo restores multiple choices in reverse order after serialization', () => {
  let library = new Library();
  library.choose('sample-1', 'keep'); library.choose('sample-2', 'trash');
  library = new Library(library.serialize());
  assert.equal(library.undo(), 'sample-2'); assert.equal(library.undo(), 'sample-1');
  assert.equal(library.canUndo(), false); assert.equal(library.counts().unassigned, 9);
});
test('skip is not counted as sorted and revisit clears incompatible undo', () => {
  const library = new Library(); library.choose('sample-1', 'skip'); library.choose('sample-2', 'keep');
  assert.equal(library.counts().sorted, 1); library.revisit();
  assert.equal(library.counts().skip, 0); assert.equal(library.counts().unassigned, 8); assert.equal(library.canUndo(), false);
});
test('hiding and restoring samples preserves hidden choices', () => {
  const library = new Library(); library.choose('sample-1', 'keep'); library.toggleSamples(false); library.reset();
  assert.equal(library.visible().length, 0); library.toggleSamples(true); assert.equal(library.counts().keep, 1);
});
test('combined search, status, and source filters are case insensitive', () => {
  const library = new Library(); library.choose('sample-1', 'keep');
  assert.equal(library.filter(' RIDE ', 'keep', 'sample').length, 1);
  assert.equal(library.filter('ride', 'trash').length, 0); assert.equal(library.filter('', 'all', 'imported').length, 0);
});
test('imports stay in memory and duplicate IDs are rejected', () => {
  const library = new Library();
  assert.equal(library.add({ id:'import-abc', name:'private.jpg', url:'blob:local' }), true);
  assert.equal(library.add({ id:'import-abc', name:'copy.jpg', url:'blob:other' }), false);
  library.choose('import-abc', 'keep');
  const data = library.serialize(); assert.equal(JSON.stringify(data).includes('private.jpg'), false);
  assert.equal(new Library(data).photos.length, 9);
});
test('only imported session photos can be removed and unrelated undo survives', () => {
  const library = new Library(); library.add({ id:'import-abc', name:'a.jpg', url:'blob:a' });
  library.choose('sample-1', 'keep'); library.choose('import-abc', 'trash');
  assert.equal(library.remove('sample-1'), false); assert.equal(library.remove('import-abc'), true);
  assert.equal(library.undo(), 'sample-1'); assert.equal(library.counts().unassigned, 9);
});
test('malformed persisted values fall back to usable defaults', () => {
  const library = new Library({statuses:{'sample-1':'invalid'},theme:'unknown',history:[null,{id:'unknown',before:'keep',after:'trash'}]});
  assert.equal(library.theme, 'system'); assert.equal(library.counts().unassigned, 9); assert.equal(library.canUndo(), false);
  assert.equal(new Library(null).photos.length, 9);
});
test('invalid choices and missing photos leave library unchanged', () => {
  const library = new Library(); assert.equal(library.choose('missing','keep'), false); assert.equal(library.choose('sample-1','invalid'), false);
  assert.equal(library.history.length, 0); assert.equal(library.photos.length, SAMPLES.length);
});
test('CSV quotes filenames and protects spreadsheet formulas', () => {
  assert.equal(csvField('trip, "summer".jpg'), '"trip, ""summer"".jpg"');
  assert.equal(csvField(' =SUM(1,2)'), '"\' =SUM(1,2)"');
  assert.equal(toCsv([{name:'a.jpg',status:'keep'}]), 'fileName,status\r\n"a.jpg",KEEP\r\n');
});
