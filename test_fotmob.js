const https = require('https');

https.get('https://www.fotmob.com/match/5225665', {
  headers: {
    'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)'
  }
}, (res) => {
  let data = '';
  res.on('data', (chunk) => { data += chunk; });
  res.on('end', () => {
    const fs = require('fs');
    fs.writeFileSync('match.html', data);
  });
});
