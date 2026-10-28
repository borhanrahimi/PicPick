import { Library, SAMPLES, STATUSES, toCsv } from './model.js';

const STORAGE_KEY = 'picpick-demo-v1';
const $ = selector => document.querySelector(selector);
const icon = name => `<svg class="icon" aria-hidden="true"><use href="#i-${name}"/></svg>`;
const escape = value => String(value).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
const labels = { unassigned: 'To review', keep: 'Keep', trash: 'Trash', skip: 'Skip' };
let storageBlocked = false, saved;
try { saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}'); } catch { storageBlocked = true; }
const library = new Library(saved);
let route = 'review', currentId = null, query = '', filter = 'all', source = 'all', importing = false;
let toastTimeout, confirmAction, pointer, suppressPhotoClickUntil = 0;
let zoom = 1, panX = 0, panY = 0, viewerPointer = null;
const systemTheme = window.matchMedia('(prefers-color-scheme: dark)');
const contributors = [
  ['Borhan Rahimi', 'https://www.linkedin.com/in/borhanrahimi/'],
  ['Nadibur Rahman', 'https://www.linkedin.com/in/nadibur-rahman/'],
  ['Simon Karottakuttu', 'https://www.linkedin.com/in/simon-karottakuttu/'],
  ['Levi Torres', 'https://www.linkedin.com/in/torres-levi/'],
];

function persist() {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(library.serialize())); }
  catch { storageBlocked = true; }
}
function applyTheme() {
  document.documentElement.dataset.theme = library.theme === 'system' ? (systemTheme.matches ? 'dark' : 'light') : library.theme;
  $('meta[name="theme-color"]').content = document.documentElement.dataset.theme === 'dark' ? '#17151f' : '#f7f5fc';
}
systemTheme.addEventListener('change', applyTheme);
function notify(message) {
  clearTimeout(toastTimeout);
  $('#toast').textContent = message; $('#toast').classList.add('visible');
  toastTimeout = setTimeout(() => $('#toast').classList.remove('visible'), 3600);
}
function header(title, subtitle, add = true) {
  return `<header class="panel-header"><div><h2>${title}</h2><p>${subtitle}</p></div>${add ? `<button class="button secondary small" data-action="import" ${importing ? 'disabled' : ''}>${icon('plus')}${importing ? 'Adding…' : 'Add photos'}</button>` : ''}</header>`;
}
function renderNavigation() {
  const count = library.counts().unassigned;
  $('#navigation').innerHTML = [['review', 'Review', 'review'], ['gallery', 'Gallery', 'grid'], ['folders', 'Folders', 'folder'], ['settings', 'Settings', 'settings']]
    .map(([key, label, glyph]) => `<button class="nav-button" data-route="${key}" ${route === key ? 'aria-current="page"' : ''}>${icon(glyph)}<span>${label}</span>${key === 'review' ? `<span class="nav-badge">${count}</span>` : ''}</button>`).join('');
}
function renderInspector() {
  const counts = library.counts(), percentage = counts.total ? Math.round(counts.sorted / counts.total * 100) : 0;
  $('#inspector').innerHTML = `<h3>A little progress.</h3><div class="progress-donut" style="--progress:${percentage}%" role="img" aria-label="${counts.sorted} of ${counts.total} photos sorted"><div><strong>${percentage}%</strong><span>ALL SORTED</span></div></div><p class="progress-label">${counts.sorted} of ${counts.total} photos, decided.</p>
    ${['keep', 'trash', 'skip', 'unassigned'].map(status => `<div class="count-row"><span class="status-dot ${status}"></span><span>${labels[status]}</span><strong data-count="${status}">${counts[status]}</strong></div>`).join('')}
    <div class="tiny-card">${icon('photo')}<strong>Every photo has a story.</strong><p>No need to decide everything now. Your skipped photos will be here when you're ready.</p></div>
    <button class="text-button" data-action="guide">${icon('review')}How does it work?</button><button class="text-button" data-action="export">${icon('download')}Download sorting report</button>`;
}
function currentPhoto() {
  const queue = library.visible().filter(p => p.status === 'unassigned');
  const current = queue.find(p => p.id === currentId) || queue[0];
  currentId = current?.id || null; return current;
}
function renderReview() {
  const counts = library.counts(), photo = currentPhoto();
  let body;
  if (photo) {
    const index = library.visible().indexOf(photo) + 1;
    body = `<div class="review-overline"><strong>One moment at a time</strong><span>${counts.unassigned} left to review</span></div>
      <button class="photo-stage" data-action="view-current" aria-label="Open ${escape(photo.name)} for a closer look"><img src="${escape(photo.url)}" alt="${escape(photo.alt || photo.name)}" draggable="false" fetchpriority="high"><span class="sample-pill">${photo.source === 'sample' ? 'SAMPLE COLLECTION' : 'YOUR PHOTO · THIS SESSION'}</span><span class="expand-badge">${icon('expand')}</span></button>
      <div class="photo-name"><strong>${escape(photo.name)}</strong><span>${String(index).padStart(2, '0')} / ${String(counts.total).padStart(2, '0')}</span></div>`;
  } else {
    const skipped = counts.skip > 0;
    body = `<div class="empty-state"><div class="empty-symbol">${icon(counts.total ? 'check' : 'photo')}</div><h3>${!counts.total ? 'A fresh start.' : skipped ? 'The maybes can wait.' : 'A little lighter already.'}</h3><p>${!counts.total ? 'Add a few photos or bring back the sample collection to start exploring.' : skipped ? `You've finished your first pass. ${counts.skip} skipped ${counts.skip === 1 ? 'photo is' : 'photos are'} ready whenever you are.` : 'Every photo has a place. Take a look at the moments you decided to keep.'}</p>
      ${skipped ? `<button class="button primary" data-action="revisit">Review skipped photos ${icon('arrow')}</button>` : counts.total ? `<button class="button primary" data-route="folders">See your collection ${icon('folder')}</button>` : `<button class="button primary" data-action="import">${icon('plus')} Add photos</button>${!library.showSamples ? '<button class="text-button" data-action="restore-samples">Use sample photos instead</button>' : ''}`}</div>`;
  }
  return header('A fresh perspective.', 'Keep the moments. Make a little room.') + body + `<div class="review-actions" aria-label="Photo sorting actions">
    ${[['trash','Trash','trash'],['skip','Skip','skip'],['undo','Undo','undo'],['keep','Keep','check']].map(([action,label,glyph]) => `<button class="decision ${action}" data-action="${action}" ${action === 'undo' ? (!library.canUndo() ? 'disabled' : '') : !photo ? 'disabled' : ''}><span class="decision-circle">${icon(glyph)}</span><span>${label}</span></button>`).join('')}
    </div><div class="keyboard-hint"><span>A little shortcut:</span><kbd>←</kbd><span>trash</span><kbd>→</kbd><span>keep</span><kbd>S</kbd><span>skip</span><kbd>Z</kbd><span>undo</span></div>`;
}
function renderGallery() {
  return header('Your gallery.', 'Good moments, all in one place.') + `<div class="gallery-controls"><label class="search-box">${icon('search')}<span class="sr-only">Search photo names</span><input id="search" type="search" placeholder="Find a moment…" value="${escape(query)}" autocomplete="off"></label><label><span class="sr-only">Photo source</span><select id="source-filter"><option value="all" ${source === 'all' ? 'selected' : ''}>All photos</option><option value="sample" ${source === 'sample' ? 'selected' : ''}>Samples</option><option value="imported" ${source === 'imported' ? 'selected' : ''}>Your photos</option></select></label></div>
    <div class="filter-row" aria-label="Filter by sorting status">${['all', ...STATUSES].map(status => `<button class="filter-button ${filter === status ? 'active' : ''}" data-filter="${status}" aria-pressed="${filter === status}">${status === 'all' ? 'All photos' : labels[status]}</button>`).join('')}</div><div id="gallery-results"></div>`;
}
function renderGalleryResults() {
  const photos = library.filter(query, filter, source);
  $('#gallery-results').innerHTML = `<p class="results-count" role="status">${photos.length} ${photos.length === 1 ? 'photo' : 'photos'}</p>` + (photos.length ? `<div class="gallery-grid">${photos.map(p => `<article class="gallery-card"><button class="gallery-image" data-view="${escape(p.id)}" aria-label="View ${escape(p.name)}"><img src="${escape(p.url)}" alt="${escape(p.alt || p.name)}" loading="lazy"><span class="gallery-status ${p.status}">${labels[p.status]}</span></button><h3 class="gallery-caption" title="${escape(p.name)}">${escape(p.name)}</h3><div class="gallery-source">${p.source === 'sample' ? 'Sample collection' : 'This session only'}</div>${p.source === 'imported' ? `<button class="remove-small" data-remove="${escape(p.id)}">Remove from demo</button>` : ''}</article>`).join('')}</div>` : `<div class="empty-state"><div class="empty-symbol">${icon('search')}</div><h3>No moments found.</h3><p>Try another search, change the filters, or add a few photos of your own.</p><button class="button secondary" data-action="clear-filters">Clear filters</button></div>`);
}
function renderFolders() {
  const counts = library.counts();
  return header('Your collection.', 'Every decision has a place.', false) + `<div class="collection-hero"><p class="eyebrow">A LITTLE MORE ORGANIZED</p><h3>${counts.total} photos. ${counts.sorted} decisions.</h3><p>${counts.unassigned + counts.skip} still to review.<br>Keep going, or enjoy what you've kept.</p></div><div class="folder-list">
    ${[['keep','check','The moments that matter'],['trash','trash','Marked, never deleted'],['skip','skip','A little time to decide']].map(([status,glyph,description]) => `<button class="folder-card ${status}" data-folder="${status}"><span class="folder-icon">${icon(glyph)}</span><span class="folder-text"><strong>${labels[status]}</strong><span>${description}</span></span><strong class="folder-total">${counts[status]}</strong><svg class="icon folder-chevron"><use href="#i-arrow"/></svg></button>`).join('')}</div><div class="folder-footer"><button class="button secondary" data-action="export">${icon('download')}Download report</button><button class="text-button" data-action="reset">Start fresh</button></div>`;
}
function renderSettings() {
  const imported = library.photos.filter(p => p.source === 'imported');
  return header('Make it yours.', 'Your photos. Your pace. Your preferences.', false) +
    `<section class="settings-section"><h3 class="section-label">APPEARANCE</h3><fieldset class="appearance-options"><legend class="sr-only">Choose appearance</legend>${[['system','photo','System'],['light','sun','Light'],['dark','moon','Dark']].map(([value,glyph,label]) => `<label class="appearance-option"><input type="radio" name="theme" value="${value}" ${library.theme === value ? 'checked' : ''}>${icon(glyph)}${label}</label>`).join('')}</fieldset></section>
    <section class="settings-section"><h3 class="section-label">YOUR COLLECTION</h3><label class="toggle-label">Include sample photos<input id="samples-toggle" type="checkbox" ${library.showSamples ? 'checked' : ''}></label><p>Explore PicPick with nine sample images. Hiding them keeps their saved choices.</p><div class="settings-actions"><button class="button secondary" data-action="import" ${importing ? 'disabled' : ''}>${icon('plus')}${importing ? 'Adding…' : 'Add your photos'}</button><button class="button secondary" data-action="manage-imports">${icon('folder')}Session photos (${imported.length})</button></div><p class="storage-note">Your own photos stay in memory for this session and disappear on reload. Nothing is uploaded. Sample sorting choices save in this browser.</p></section>
    <section class="settings-section"><h3 class="section-label">A FRESH START</h3><div class="setting-row"><strong>Reset sorting choices</strong><button class="button secondary small" data-action="reset">Reset demo</button></div><p>Return visible photos to review and clear undo history. Your original files stay untouched.</p><div class="settings-actions"><button class="button soft" data-action="guide">${icon('review')}How to use PicPick</button><button class="button secondary" data-action="export">${icon('download')}Download CSV</button></div></section>
    <section class="settings-section"><h3 class="section-label">BUILT TOGETHER</h3><div class="settings-credits">${contributors.map(([name,url]) => `<a href="${url}" target="_blank" rel="noopener noreferrer">${name}<span aria-hidden="true">↗</span></a>`).join('')}</div><p>Created with the contributions of all four team members.</p></section>`;
}
function render({ focus } = {}) {
  applyTheme(); renderNavigation(); renderInspector();
  $('#main-panel').innerHTML = (storageBlocked ? '<p class="memory-warning">Browser storage is unavailable. The demo still works, but choices may reset when you reload.</p>' : '') + ({ review: renderReview, gallery: renderGallery, folders: renderFolders, settings: renderSettings }[route])();
  if (route === 'gallery') renderGalleryResults();
  if (focus) $(`[data-action="${focus}"]`)?.focus({ preventScroll: true });
}
function navigate(nextRoute) {
  if (!['review', 'gallery', 'folders', 'settings'].includes(nextRoute)) return;
  route = nextRoute; render();
}
function choose(status) {
  const photo = currentPhoto(); if (!photo || !library.choose(photo.id, status)) return;
  currentId = null; persist(); render({ focus: status });
  notify(status === 'keep' ? 'A keeper. On to the next moment.' : status === 'trash' ? 'Marked as Trash. Your original stays untouched.' : 'Saved for later. No rush.');
}
function confirm(title, description, button, action) {
  $('#confirm-title').textContent = title; $('#confirm-description').textContent = description;
  $('#confirm-submit').textContent = button; confirmAction = action;
  $('#confirm-dialog').returnValue = ''; $('#confirm-dialog').showModal();
}
$('#confirm-dialog').addEventListener('close', () => {
  const action = confirmAction; confirmAction = null;
  if ($('#confirm-dialog').returnValue === 'confirm') action?.();
});
function resetChoices() {
  confirm('A fresh start?', 'All visible photos will return to the review queue and undo history will be cleared. Your files stay untouched.', 'Reset choices', () => {
    library.reset(); currentId = null; persist(); render(); notify('A clean slate. Your photos are ready to review.');
  });
}
function exportReport() {
  const url = URL.createObjectURL(new Blob([toCsv(library.visible())], { type: 'text/csv;charset=utf-8' }));
  const link = document.createElement('a'); link.href = url; link.download = 'picpick-sorting-report.csv';
  document.body.append(link); link.click(); link.remove(); setTimeout(() => URL.revokeObjectURL(url), 1000);
  notify('Your sorting report is ready.');
}
function openViewer(id) {
  const photo = library.visible().find(p => p.id === id); if (!photo) return;
  $('#photo-title').textContent = photo.name; $('#viewer-image').src = photo.url; $('#viewer-image').alt = photo.alt || photo.name;
  zoom = 1; panX = panY = 0; updateZoom(); $('#photo-dialog').showModal();
}
function updateZoom() {
  $('#viewer-image').style.transform = `translate(${panX}px,${panY}px) scale(${zoom})`;
  $('#zoom-label').textContent = `${Math.round(zoom * 100)}%`;
  $('#zoom-stage').style.cursor = zoom > 1 ? 'grab' : 'zoom-in';
}
function setZoom(value) {
  zoom = Math.max(1, Math.min(4, value)); if (zoom === 1) panX = panY = 0; clampPan(); updateZoom();
}
function clampPan() {
  const image = $('#viewer-image'), stage = $('#zoom-stage');
  const dx = Math.max(0, (image.offsetWidth * zoom - stage.clientWidth) / 2), dy = Math.max(0, (image.offsetHeight * zoom - stage.clientHeight) / 2);
  panX = Math.max(-dx, Math.min(dx, panX)); panY = Math.max(-dy, Math.min(dy, panY));
}
function removePhoto(id) {
  const photo = library.photos.find(p => p.id === id && p.source === 'imported'); if (!photo) return;
  confirm('Remove from this demo?', `Remove “${photo.name}” and its demo choices? Your original file stays untouched.`, 'Remove photo', () => {
    library.remove(id); URL.revokeObjectURL(photo.url); persist(); render(); notify('Removed from this session. Your original is untouched.');
  });
}
function action(name) {
  if (['keep','trash','skip'].includes(name)) return choose(name);
  if (name === 'undo') { currentId = library.undo(); persist(); render({ focus: 'undo' }); return notify('Last choice restored.'); }
  if (name === 'import') { if (!importing) $('#file-input').click(); }
  if (name === 'reset') resetChoices();
  if (name === 'guide') $('#guide-dialog').showModal();
  if (name === 'export') exportReport();
  if (name === 'view-current' && performance.now() > suppressPhotoClickUntil) openViewer(currentId);
  if (name === 'close-viewer') $('#photo-dialog').close();
  if (name === 'zoom-in') setZoom(zoom + .5);
  if (name === 'zoom-out') setZoom(zoom - .5);
  if (name === 'zoom-reset') setZoom(1);
  if (name === 'clear-filters') { query = ''; filter = source = 'all'; render(); }
  if (name === 'manage-imports') { query = ''; filter = 'all'; source = 'imported'; navigate('gallery'); }
  if (name === 'restore-samples') { library.toggleSamples(true); persist(); render(); }
  if (name === 'revisit') { library.revisit(); currentId = null; persist(); render(); notify('Your skipped photos are ready for another look.'); }
}
document.addEventListener('click', event => {
  const button = event.target.closest('button, a'); if (!button || button.disabled) return;
  if (button.dataset.route) return navigate(button.dataset.route);
  if (button.dataset.action) return action(button.dataset.action);
  if (button.dataset.filter) { filter = button.dataset.filter; render(); return; }
  if (button.dataset.folder) { filter = button.dataset.folder; source = 'all'; query = ''; navigate('gallery'); return; }
  if (button.dataset.view) openViewer(button.dataset.view);
  if (button.dataset.remove) removePhoto(button.dataset.remove);
});
document.addEventListener('input', event => { if (event.target.id === 'search') { query = event.target.value; renderGalleryResults(); } });
document.addEventListener('change', event => {
  if (event.target.id === 'source-filter') { source = event.target.value; renderGalleryResults(); }
  if (event.target.name === 'theme') { library.theme = event.target.value; persist(); applyTheme(); }
  if (event.target.id === 'samples-toggle') { library.toggleSamples(event.target.checked); currentId = null; persist(); render(); }
});
document.addEventListener('keydown', event => {
  if (route !== 'review' || document.querySelector('dialog[open]') || event.altKey || event.metaKey || event.ctrlKey || /INPUT|SELECT|TEXTAREA/.test(event.target.tagName)) return;
  const key = event.key.toLowerCase(), name = { arrowleft:'trash', arrowright:'keep', s:'skip', z:'undo' }[key];
  if (name && (name !== 'undo' || library.canUndo()) && (name === 'undo' || currentPhoto())) { event.preventDefault(); action(name); }
});
$('#main-panel').addEventListener('pointerdown', event => {
  const stage = event.target.closest('.photo-stage');
  if (stage && event.isPrimary) { pointer = { x:event.clientX, y:event.clientY, stage }; stage.setPointerCapture(event.pointerId); }
});
$('#main-panel').addEventListener('pointerup', event => {
  if (!pointer) return;
  const dx = event.clientX - pointer.x, dy = event.clientY - pointer.y;
  pointer = null;
  if (Math.abs(dx) >= 70 && Math.abs(dx) > Math.abs(dy) * 1.5) {
    suppressPhotoClickUntil = performance.now() + 400; choose(dx > 0 ? 'keep' : 'trash');
  }
});
$('#main-panel').addEventListener('pointercancel', () => { pointer = null; });
$('#zoom-stage').addEventListener('wheel', event => { event.preventDefault(); setZoom(zoom + (event.deltaY < 0 ? .25 : -.25)); }, { passive:false });
$('#zoom-stage').addEventListener('dblclick', () => setZoom(zoom > 1 ? 1 : 2.5));
$('#zoom-stage').addEventListener('pointerdown', event => {
  if (zoom <= 1) return;
  viewerPointer = { x:event.clientX, y:event.clientY }; event.currentTarget.setPointerCapture(event.pointerId);
});
$('#zoom-stage').addEventListener('pointermove', event => {
  if (!viewerPointer) return;
  panX += event.clientX - viewerPointer.x; panY += event.clientY - viewerPointer.y;
  viewerPointer = { x:event.clientX, y:event.clientY }; clampPan(); updateZoom();
});
for (const type of ['pointerup','pointercancel']) $('#zoom-stage').addEventListener(type, () => { viewerPointer = null; });
$('#photo-dialog').addEventListener('close', () => { viewerPointer = null; });

async function digest(file) {
  const bytes = new Uint8Array(await crypto.subtle.digest('SHA-256', await file.arrayBuffer()));
  return [...bytes].map(value => value.toString(16).padStart(2, '0')).join('');
}
function validateImage(url) {
  return new Promise((resolve, reject) => {
    const image = new Image(); image.onload = () => image.naturalWidth * image.naturalHeight <= 100000000 ? resolve() : reject(new Error('Image too large'));
    image.onerror = reject; image.src = url;
  });
}
$('#file-input').addEventListener('change', async event => {
  const files = [...event.target.files]; event.target.value = ''; if (!files.length || importing) return;
  importing = true; render(); let added = 0, duplicates = 0, failed = Math.max(0, files.length - 30);
  for (const file of files.slice(0,30)) {
    let url;
    try {
      if (!file.type.startsWith('image/') || file.size > 25 * 1024 * 1024) throw new Error('Unsupported image');
      const id = 'import-' + await digest(file);
      if (library.photos.some(p => p.id === id)) { duplicates++; continue; }
      url = URL.createObjectURL(file); await validateImage(url);
      library.add({ id, name:file.name, alt:file.name, url }); added++;
    } catch { if (url) URL.revokeObjectURL(url); failed++; }
  }
  importing = false; render(); notify(`${added} added · ${duplicates} duplicates · ${failed} could not be added. Your photos stay in this session.`);
});
applyTheme(); render();
// Preload the next sample without fetching any third-party resources.
const next = library.visible().filter(p => p.status === 'unassigned')[1];
if (next) { const image = new Image(); image.src = next.url; }
