const https = require('https');

https.get('https://www.fotmob.com/api/matchDetails?matchId=5225665', {
  headers: {
    'User-Agent': 'Mozilla/5.0'
  }
}, (res) => {
  let data = '';
  res.on('data', (chunk) => { data += chunk; });
  res.on('end', () => {
    console.log(res.statusCode);
    console.log(data.slice(0, 100));
  });
});
