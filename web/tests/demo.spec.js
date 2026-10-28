import { test, expect } from '@playwright/test';
import { readFile } from 'node:fs/promises';
const panel = page => page.locator('#main-panel');
const navigate = (page, name) => page.locator(`#navigation [data-route="${name}"]`).click();
const pageErrors = new WeakMap();
test.beforeEach(async ({page}) => {
  const errors = []; pageErrors.set(page, errors);
  page.on('pageerror', error => errors.push(error.message));
  await page.goto('./');
});
test.afterEach(async ({page}) => expect(pageErrors.get(page)).toEqual([]));

test('sorts, skips, undoes and persists sample decisions', async ({page}) => {
  await panel(page).getByRole('button',{name:'Keep',exact:true}).click();
  await panel(page).getByRole('button',{name:'Trash',exact:true}).click();
  await panel(page).getByRole('button',{name:'Skip',exact:true}).click();
  await panel(page).getByRole('button',{name:'Undo',exact:true}).click();
  await page.reload();
  await expect(panel(page).getByText('7 left to review')).toBeVisible();
  await panel(page).getByRole('button',{name:'Undo',exact:true}).click();
  await panel(page).getByRole('button',{name:'Undo',exact:true}).click();
  await expect(panel(page).getByText('9 left to review')).toBeVisible();
  await expect(panel(page).getByRole('button',{name:'Undo',exact:true})).toBeDisabled();
});
test('gallery filters combine with search and folder navigation', async ({page}) => {
  await panel(page).getByRole('button',{name:'Keep',exact:true}).click();
  await navigate(page,'folders');
  await panel(page).locator('[data-folder="keep"]').click();
  await expect(panel(page).getByRole('status')).toHaveText('1 photo');
  await panel(page).getByRole('searchbox').fill('RIDE');
  await expect(panel(page).locator('.gallery-card')).toHaveCount(1);
  await panel(page).getByRole('searchbox').fill('not a photo');
  await expect(panel(page).getByText('No moments found.')).toBeVisible();
  await panel(page).getByRole('button',{name:'Clear filters'}).click();
  await expect(panel(page).locator('.gallery-card')).toHaveCount(9);
});
test('reset requires confirmation and cancellation preserves choices', async ({page}) => {
  await panel(page).getByRole('button',{name:'Keep',exact:true}).click();
  await navigate(page,'settings'); await panel(page).getByRole('button',{name:'Reset demo',exact:true}).click();
  await page.getByRole('button',{name:'Cancel',exact:true}).click();
  await navigate(page,'review'); await expect(panel(page).getByText('8 left to review')).toBeVisible();
  await navigate(page,'settings'); await panel(page).getByRole('button',{name:'Reset demo',exact:true}).click();
  await page.getByRole('button',{name:'Reset choices',exact:true}).click();
  await navigate(page,'review'); await expect(panel(page).getByText('9 left to review')).toBeVisible();
});
test('theme persists and all four contributors have their links', async ({page}) => {
  await navigate(page,'settings'); await page.getByRole('radio',{name:'Dark',exact:true}).check();
  await expect(page.locator('html')).toHaveAttribute('data-theme','dark');
  for (const [name,path] of [['Borhan Rahimi','borhanrahimi'],['Nadibur Rahman','nadibur-rahman'],['Simon Karottakuttu','simon-karottakuttu'],['Levi Torres','torres-levi']]) {
    await expect(panel(page).getByRole('link',{name})).toHaveAttribute('href',`https://www.linkedin.com/in/${path}/`);
  }
  await page.reload(); await expect(page.locator('html')).toHaveAttribute('data-theme','dark');
});
test('photo viewer opens, zooms, resets, and closes with Escape', async ({page}) => {
  await panel(page).locator('.photo-stage').click();
  await expect(page.getByRole('dialog',{name:'The ride home'})).toBeVisible();
  await page.getByRole('button',{name:'Zoom in',exact:true}).click();
  await expect(page.locator('#zoom-label')).toHaveText('150%');
  await page.locator('#zoom-label').click(); await expect(page.locator('#zoom-label')).toHaveText('100%');
  await page.keyboard.press('Escape'); await expect(page.locator('#photo-dialog')).not.toBeVisible();
});
test('CSV downloads contain the current sorting choices', async ({page}) => {
  await panel(page).getByRole('button',{name:'Keep',exact:true}).click(); await navigate(page,'folders');
  const downloadPromise = page.waitForEvent('download');
  await panel(page).getByRole('button',{name:'Download report',exact:true}).click();
  const download = await downloadPromise; expect(download.suggestedFilename()).toBe('picpick-sorting-report.csv');
  const text = await readFile(await download.path(),'utf8'); expect(text).toContain('"The ride home",KEEP');
});
test('personal imports deduplicate, remove safely, and disappear on reload', async ({page}) => {
  await page.locator('#file-input').setInputFiles(['assets/sample-01.webp','assets/sample-01.webp']);
  await expect(page.getByRole('status').filter({hasText:'1 added'})).toBeVisible();
  await navigate(page,'settings'); await panel(page).getByRole('button',{name:'Session photos (1)'}).click();
  await expect(panel(page).locator('.gallery-card')).toHaveCount(1);
  await panel(page).getByRole('button',{name:'Remove from demo'}).click();
  await page.getByRole('button',{name:'Cancel',exact:true}).click();
  await expect(panel(page).locator('.gallery-card')).toHaveCount(1);
  await panel(page).getByRole('button',{name:'Remove from demo'}).click();
  await page.getByRole('button',{name:'Remove photo',exact:true}).click();
  await expect(panel(page).locator('.gallery-card')).toHaveCount(0);
  await page.reload(); await navigate(page,'gallery'); await expect(panel(page).locator('.gallery-card')).toHaveCount(9);
});
test('layout stays within the viewport and has no broken sample images', async ({page}) => {
  await navigate(page,'gallery');
  await expect(panel(page).locator('.gallery-card')).toHaveCount(9);
  await panel(page).locator('.gallery-card').last().scrollIntoViewIfNeeded();
  await expect.poll(() => page.locator('.gallery-image img').evaluateAll(images => images.filter(image=>image.complete && image.naturalWidth>0).length)).toBe(9);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  await navigate(page,'settings'); expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
});
test('storage denial does not prevent sorting', async ({page}) => {
  await page.addInitScript(()=>Object.defineProperty(window,'localStorage',{get(){throw new Error('Storage unavailable');}}));
  await page.reload(); await expect(panel(page).getByText(/Browser storage is unavailable/)).toBeVisible();
  await panel(page).getByRole('button',{name:'Keep',exact:true}).click();
  await expect(panel(page).getByText('8 left to review')).toBeVisible();
});

test('horizontal gestures sort exactly once without opening the viewer', async ({page}) => {
  const swipe = async direction => {
    const box = await panel(page).locator('.photo-stage').boundingBox();
    const start = direction > 0 ? .2 : .8, end = direction > 0 ? .8 : .2;
    await page.mouse.move(box.x + box.width * start, box.y + box.height / 2);
    await page.mouse.down(); await page.mouse.move(box.x + box.width * end, box.y + box.height / 2, {steps:12}); await page.mouse.up();
  };
  await swipe(1); await expect(panel(page).getByText('8 left to review')).toBeVisible();
  await expect(page.locator('#photo-dialog')).not.toBeVisible();
  await swipe(-1); await expect(panel(page).getByText('7 left to review')).toBeVisible();
  await expect(page.locator('#photo-dialog')).not.toBeVisible();
});
test('finishing a pass offers skipped review and then completion', async ({page}) => {
  await panel(page).getByRole('button',{name:'Skip',exact:true}).click();
  for (let i=0;i<8;i++) await panel(page).getByRole('button',{name:'Keep',exact:true}).click();
  await expect(panel(page).getByText('The maybes can wait.')).toBeVisible();
  await panel(page).getByRole('button',{name:'Review skipped photos'}).click();
  await expect(panel(page).getByText('1 left to review')).toBeVisible();
  await expect(panel(page).getByRole('button',{name:'Undo',exact:true})).toBeDisabled();
  await panel(page).getByRole('button',{name:'Keep',exact:true}).click();
  await expect(panel(page).getByText('A little lighter already.')).toBeVisible();
  await expect(panel(page).getByRole('button',{name:'Keep',exact:true})).toBeDisabled();
});
test('reload discards personal photos without persisting their filenames', async ({page}) => {
  await page.locator('#file-input').setInputFiles('assets/sample-01.webp');
  await expect(page.getByRole('status').filter({hasText:'1 added'})).toBeVisible();
  await panel(page).getByRole('button',{name:'Keep',exact:true}).click();
  expect(await page.evaluate(()=>localStorage.getItem('picpick-demo-v1'))).not.toContain('sample-01.webp');
  await page.reload(); await navigate(page,'gallery');
  await panel(page).getByRole('combobox',{name:'Photo source'}).selectOption('imported');
  await expect(panel(page).locator('.gallery-card')).toHaveCount(0);
});
