const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const ROOT_DIR = __dirname;

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.ttf': 'font/ttf',
  '.otf': 'font/otf',
  '.apk': 'application/vnd.android.package-archive',
  '.sha256': 'text/plain; charset=utf-8'
};

const server = http.createServer((req, res) => {
  // CORS & Security headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const urlPath = req.url.split('?')[0];

  // Health check
  if (urlPath === '/health' || urlPath === '/_health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', app: 'GlassNotes', port: PORT }));
    return;
  }

  // Potential file candidates
  let candidatePaths = [];

  if (urlPath === '/' || urlPath === '/index.html') {
    candidatePaths.push(path.join(ROOT_DIR, 'index.html'));
    candidatePaths.push(path.join(ROOT_DIR, 'public', 'assets', 'aistudio', 'Notes__1__b46aed40.html'));
  } else if (urlPath.endsWith('.apk') || urlPath.includes('GlassNotes.apk') || urlPath.includes('app-release.apk')) {
    // Check built APK locations
    candidatePaths.push(path.join(ROOT_DIR, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk'));
    candidatePaths.push(path.join(ROOT_DIR, 'app', 'build', 'outputs', 'apk', 'release', 'app-release.apk'));
    candidatePaths.push(path.join(ROOT_DIR, 'GlassNotes.apk'));
    candidatePaths.push(path.join(ROOT_DIR, 'GlassNotes.apk.idsig'));
  } else {
    // Direct or public path
    candidatePaths.push(path.join(ROOT_DIR, urlPath));
    candidatePaths.push(path.join(ROOT_DIR, 'public', urlPath));
    candidatePaths.push(path.join(ROOT_DIR, 'public', 'assets', urlPath));
    candidatePaths.push(path.join(ROOT_DIR, 'app', 'src', 'main', 'assets', urlPath));
  }

  // Find first existing file
  let resolvedFile = null;
  for (const candidate of candidatePaths) {
    try {
      if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
        resolvedFile = candidate;
        break;
      }
    } catch (_) {}
  }

  // If not found and looks like a page request, serve index.html (SPA fallback)
  if (!resolvedFile && !path.extname(urlPath)) {
    const indexPath = path.join(ROOT_DIR, 'index.html');
    if (fs.existsSync(indexPath)) {
      resolvedFile = indexPath;
    }
  }

  if (resolvedFile) {
    const ext = path.extname(resolvedFile).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';
    const stream = fs.createReadStream(resolvedFile);
    res.writeHead(200, {
      'Content-Type': contentType,
      'Cache-Control': 'no-cache'
    });
    stream.pipe(res);
  } else {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('404 Not Found');
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`GlassNotes dev server running at http://0.0.0.0:${PORT}/`);
});
