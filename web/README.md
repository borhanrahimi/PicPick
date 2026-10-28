# PicPick web demo

An interactive, static web adaptation of the native Android project. Visitors can try sample sorting, undo, folders, search, themes, session-only imports, a photo viewer, and CSV export. All four original contributors are credited with their LinkedIn profiles.

## Preview locally

From the repository root:

```sh
npm --prefix web start
```

Open `http://127.0.0.1:4173`. Serving needs Node.js 22+; the published site runs as plain HTML/CSS/JavaScript and does not need a Node server.

## Publish on GitHub Pages

The repository includes `.github/workflows/web-demo.yml`, which tests the site and deploys only the production files.

1. Commit and push the new `web/` directory, workflow, and documentation to the repository's `main` branch.
2. Open [repository Pages settings](https://github.com/borhanrahimi/PicPick/settings/pages).
3. Under **Build and deployment → Source**, select **GitHub Actions**.
4. In [Actions](https://github.com/borhanrahimi/PicPick/actions), open **Test and deploy PicPick web demo** and choose **Run workflow** if the initial push happened before Pages was enabled.
5. After deployment succeeds, open the URL shown by the deployment. For the current repository, the expected default address is `https://borhanrahimi.github.io/PicPick/`.
6. Confirm the link works in an incognito window, then use it in the repository About panel, README, portfolio, and résumé.

The URL is a publishing target until deployment succeeds. No public deployment is implied by adding these source files. GitHub Pages has hosting quotas; this demo has no streamed-emulator session timer.

See [GitHub's custom Pages workflow documentation](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages).

## Data and browser behavior

- Sample classifications, appearance, sample visibility, and recent sample undo history are saved in this site's `localStorage`.
- Personal photos use temporary browser object URLs and remain in memory for the current page session. Files, filenames, and private-image choices are not written to persistent storage or uploaded.
- Imports allow up to 30 files at a time, 25 MB and 100 megapixels per image. Duplicate personal files are identified by SHA-256 within the current session.
- Import and removal operate on browser references, never original files. Reloading discards imported photos.
- If browser storage is unavailable, the sample demo works in memory and shows a notice.
- No analytics, external fonts, third-party image requests, accounts, or API keys are used by the demo code. The static hosting provider still serves ordinary web requests.

## Tests and production package

```sh
npm --prefix web ci
npm --prefix web run check
# First-time browser installation:
cd web
npx playwright install chromium
npm run test:browser
npm run build
```

The browser suite checks desktop and mobile layouts, persistence, sorting, undo, search/filter combinations, reset confirmation, themes/credits, zoom, CSV downloads, session imports, and storage denial. Tests also exercise a `/PicPick/` URL prefix, as used by GitHub Pages.

`build.mjs` copies just the HTML, CSS, JavaScript modules, and optimized assets into ignored `web/dist/`. The Android project, tests, development dependencies, and local files are excluded from deployment.
