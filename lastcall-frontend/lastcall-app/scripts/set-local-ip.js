const os = require('os');
const fs = require('fs');
const path = require('path');

function getLocalIP() {
  const interfaces = os.networkInterfaces();
  const wifiKeywords = ['wi-fi', 'wifi', 'wlan', 'en0', 'en1', 'wlp'];
  const wifiCandidates = [];
  const otherCandidates = [];

  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        const isWifi = wifiKeywords.some(k => name.toLowerCase().includes(k));
        if (isWifi) wifiCandidates.push(iface.address);
        else otherCandidates.push(iface.address);
      }
    }
  }

  return wifiCandidates[0] || otherCandidates[0] || null;
}

const ip = getLocalIP();
if (!ip) {
  console.error('Could not detect local IP address.');
  process.exit(1);
}

const envPath = path.join(__dirname, '..', '.env.local');
let content = '';

if (fs.existsSync(envPath)) {
  content = fs.readFileSync(envPath, 'utf-8');
  if (content.match(/^EXPO_PUBLIC_API_URL=.*/m)) {
    content = content.replace(/^EXPO_PUBLIC_API_URL=.*/m, `EXPO_PUBLIC_API_URL=http://${ip}:8080`);
  } else {
    content += `\nEXPO_PUBLIC_API_URL=http://${ip}:8080`;
  }
} else {
  content = `EXPO_PUBLIC_API_URL=http://${ip}:8080\n`;
}

fs.writeFileSync(envPath, content);
console.log(`API URL set to: http://${ip}:8080`);