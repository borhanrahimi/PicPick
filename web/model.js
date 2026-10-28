export const STATUSES = ['unassigned', 'keep', 'trash', 'skip'];
export const SAMPLES = [
  ['The ride home', 'Three friends illustrated in a car'],
  ['Dinner out', 'An illustrated person enjoying a restaurant meal'],
  ['Holiday moments', 'Three people posing beside a decorated tree'],
  ['After the game', 'Two people holding orange shirts in a gym'],
  ['Campus days', 'An illustrated computer lab with friends'],
  ['A little confidence', 'An illustrated person posing in front of a mirror'],
  ['Finish-line smiles', 'Three runners wearing medals'],
  ['Good company', 'Three friends taking an outdoor selfie'],
  ['Portrait, just because', 'An illustrated portrait in a black sweater'],
].map(([name, alt], i) => ({ id: `sample-${i + 1}`, name, alt, source: 'sample', url: `./assets/sample-${String(i + 1).padStart(2, '0')}.webp` }));

export class Library {
  constructor(saved = {}) {
    this.photos = SAMPLES.map(photo => ({ ...photo, status: STATUSES.includes(saved?.statuses?.[photo.id]) ? saved.statuses[photo.id] : 'unassigned' }));
    this.history = Array.isArray(saved?.history) ? saved.history.filter(change =>
      change && this.photos.some(p => p.id === change.id) && STATUSES.includes(change.before) && STATUSES.includes(change.after)).slice(-100) : [];
    this.theme = ['system', 'light', 'dark'].includes(saved?.theme) ? saved.theme : 'system';
    this.showSamples = saved?.showSamples !== false;
  }
  visible() { return this.photos.filter(photo => this.showSamples || photo.source === 'imported'); }
  counts() {
    const result = Object.fromEntries(STATUSES.map(status => [status, 0]));
    for (const photo of this.visible()) result[photo.status]++;
    return { ...result, total: this.visible().length, sorted: result.keep + result.trash };
  }
  choose(id, status) {
    const photo = this.visible().find(p => p.id === id);
    if (!photo || !STATUSES.includes(status) || photo.status === status) return false;
    this.history.push({ id, before: photo.status, after: status });
    this.history = this.history.slice(-100); photo.status = status; return true;
  }
  canUndo() {
    while (this.history.length) {
      const last = this.history.at(-1), photo = this.visible().find(p => p.id === last.id);
      if (photo?.status === last.after) return true;
      this.history.pop();
    }
    return false;
  }
  undo() {
    if (!this.canUndo()) return null;
    const last = this.history.pop(); this.photos.find(p => p.id === last.id).status = last.before; return last.id;
  }
  reset() { this.visible().forEach(p => { p.status = 'unassigned'; }); this.history = []; }
  revisit() { this.visible().filter(p => p.status === 'skip').forEach(p => { p.status = 'unassigned'; }); this.history = []; }
  toggleSamples(show) { this.showSamples = show; this.history = []; }
  add(photo) {
    if (this.photos.some(p => p.id === photo.id)) return false;
    this.photos.push({ ...photo, source: 'imported', status: 'unassigned' }); return true;
  }
  remove(id) {
    const photo = this.photos.find(p => p.id === id && p.source === 'imported');
    if (!photo) return false;
    this.photos = this.photos.filter(p => p.id !== id); this.history = this.history.filter(change => change.id !== id); return true;
  }
  filter(query = '', status = 'all', source = 'all') {
    const needle = query.trim().toLocaleLowerCase();
    return this.visible().filter(p => (status === 'all' || p.status === status) &&
      (source === 'all' || p.source === source) && p.name.toLocaleLowerCase().includes(needle));
  }
  serialize() {
    return { statuses: Object.fromEntries(this.photos.filter(p => p.source === 'sample').map(p => [p.id, p.status])),
      history: this.history.filter(change => change.id.startsWith('sample-')), theme: this.theme, showSamples: this.showSamples };
  }
}
export function csvField(value) {
  if (/^[\s]*[=+@-]/.test(value)) value = "'" + value;
  return '"' + value.replaceAll('"', '""') + '"';
}
export function toCsv(photos) {
  return 'fileName,status\r\n' + photos.map(p => `${csvField(p.name)},${p.status.toUpperCase()}\r\n`).join('');
}
