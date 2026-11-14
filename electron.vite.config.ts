import { defineConfig, externalizeDepsPlugin } from 'electron-vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'node:path'
import { version } from './package.json'

export default defineConfig({
  main: {
    plugins: [externalizeDepsPlugin()],

    build: { rollupOptions: { input: { index: resolve('src/main/index.ts') } } },
  },
  // two preloads: the app's window, and the Quarry page's view (window.quarryHost)
  preload: { plugins: [externalizeDepsPlugin()], build: { rollupOptions: { input: { index: resolve('src/preload/index.ts'), quarry: resolve('src/preload/quarry.ts') } } } },
  // Orbit: no live reload (a reload restarts every running session), so the window only updates on Ctrl+R.
  // Orbit Next (npm run next): hot reload, React Fast Refresh swaps edited components in place and threads keep running
  // A few shared files import node:path for helpers only main calls. The build drops them; the dev server loads whole
  // files, so there it stands in a browser copy of path.
  renderer: { plugins: [react()], server: { hmr: process.env.ORBIT_NEXT === '1' },
    resolve: process.env.ORBIT_NEXT === '1' ? { alias: { 'node:path': 'path-browserify' } } : undefined, define: { __APP_VERSION__: JSON.stringify(version) } },
})
