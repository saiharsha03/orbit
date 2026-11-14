// npm run next: Orbit Next, from source beside the Orbit in daily use, with hot reload (src/shared/edition.ts SIDE).
// Page edits show as they are saved; main-process edits need Orbit Next closed and run again.
import { spawn } from 'node:child_process'

const child = spawn('npx', ['electron-vite', 'dev'], { stdio: 'inherit', shell: true, env: { ...process.env, ORBIT_NEXT: '1' } })
child.on('exit', (code) => process.exit(code ?? 0))
