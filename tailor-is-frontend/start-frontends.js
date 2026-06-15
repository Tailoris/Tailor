
const http = require('http');
const fs = require('fs');
const path = require('path');

const MIME_TYPES = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.eot': 'application/vnd.ms-fontobject'
};

function createServer(dir, port, name) {
  const server = http.createServer((req, res) => {
    let filePath = path.join(dir, req.url === '/' ? 'index.html' : req.url);
    if (!fs.existsSync(filePath)) {
      filePath = path.join(dir, 'index.html');
    }
    const extname = String(path.extname(filePath)).toLowerCase();
    const contentType = MIME_TYPES[extname] || 'application/octet-stream';
    
    fs.readFile(filePath, (error, content) => {
      if (error) {
        if(error.code === 'ENOENT') {
          const indexPath = path.join(dir, 'index.html');
          fs.readFile(indexPath, (err, indexContent) => {
            if (err) {
              res.writeHead(404);
              res.end('Not Found');
            } else {
              res.writeHead(200, { 'Content-Type': 'text/html' });
              res.end(indexContent, 'utf-8');
            }
          });
        } else {
          res.writeHead(500);
          res.end('Server error: ' + error.code);
        }
      } else {
        res.writeHead(200, { 'Content-Type': contentType });
        res.end(content, 'utf-8');
      }
    });
  });
  server.listen(port, '0.0.0.0', () => {
    console.log(`${name} running at http://0.0.0.0:${port}`);
    console.log(`  Serving from: ${dir}`);
  });
  return server;
}

const pcMallDir = path.join(__dirname, 'pc-mall', 'dist');
const merchantAdminDir = path.join(__dirname, 'merchant-admin', 'dist');

console.log('Starting Tailor IS Frontends');
console.log('=========================');
createServer(pcMallDir, 3001, 'PC Mall');
createServer(merchantAdminDir, 3002, 'Merchant Admin');
console.log('Press Ctrl+C to stop');
