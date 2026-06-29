import urllib.request
import re
import json

class RedirectHandler(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return urllib.request.Request(newurl, headers=req.headers)

opener = urllib.request.build_opener(RedirectHandler())
req = urllib.request.Request('https://www.fotmob.com/match/5205734', headers={'User-Agent': 'Mozilla/5.0'})
try:
    html = opener.open(req).read().decode('utf-8')
    m = re.search(r'__NEXT_DATA__" type="application/json">([^<]+)</script>', html)
    if m:
        data = json.loads(m.group(1))
        d = data['props']['pageProps']['fallback']
        match_data = next(v for k, v in d.items() if k.startswith('match-'))
        content = match_data['content']
        print("Keys in content:", list(content.keys()))
        if 'table' in content:
            print("Table data:", json.dumps(content['table'])[:200])
        else:
            print("No table in content.")
except Exception as e:
    print("HTTP error:", e)
