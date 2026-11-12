import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import fs from 'node:fs';
import path from 'node:path';

function syncLatestApkPlugin() {
  const syncApkFiles = () => {
    try {
      const builtApk = path.resolve('.build-outputs/app-debug.apk');
      if (fs.existsSync(builtApk)) {
        const targets = [
          path.resolve('GlassNotes.apk'),
          path.resolve('public/GlassNotes.apk'),
          path.resolve('dist/GlassNotes.apk'),
        ];
        for (const target of targets) {
          const dir = path.dirname(target);
          if (fs.existsSync(dir)) {
            fs.copyFileSync(builtApk, target);
          }
        }
      }
    } catch {
      // ignore if read-only
    }
  };

  return {
    name: 'sync-latest-apk-v1-1-18',
    buildStart() {
      syncApkFiles();
    },
    closeBundle() {
      syncApkFiles();
    },
    configureServer(server: {
      middlewares: {
        use: (
          fn: (
            req: { url?: string },
            res: {
              setHeader: (k: string, v: string | number) => void;
              end: (b: Buffer) => void;
            },
            next: () => void
          ) => void
        ) => void;
      };
    }) {
      syncApkFiles();
      server.middlewares.use((req, res, next) => {
        const cleanUrl = (req.url || '').split('?')[0];
        if (cleanUrl === '/GlassNotes.apk' || cleanUrl === '/app-release.apk') {
          syncApkFiles();
          const builtApk = path.resolve('.build-outputs/app-debug.apk');
          const fallbackApk = path.resolve('public/GlassNotes.apk');
          const sourcePath = fs.existsSync(builtApk) ? builtApk : fallbackApk;
          if (fs.existsSync(sourcePath)) {
            const buf = fs.readFileSync(sourcePath);
            res.setHeader('Content-Type', 'application/vnd.android.package-archive');
            res.setHeader(
              'Content-Disposition',
              'attachment; filename="GlassNotes-v1.1.18.apk"'
            );
            res.setHeader('Content-Length', buf.length);
            res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate');
            res.end(buf);
            return;
          }
        }
        next();
      });
    },
  };
}

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [syncLatestApkPlugin(), react(), tailwindcss()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    allowedHosts: true,
  },
});
