import urllib.request
import re
import json

class RedirectHandler(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return urllib.request.Request(newurl, headers=req.headers)

opener = urllib.request.build_opener(RedirectHandler())
req = urllib.request.Request('https://www.fotmob.com/', headers={'User-Agent': 'Mozilla/5.0'})
try:
    html = opener.open(req).read().decode('utf-8')
    m = re.search(r'__NEXT_DATA__" type="application/json">([^<]+)</script>', html)
    if m:
        data = json.loads(m.group(1))
        # Look for a match ID in page props or somewhere
        print(list(data['props']['pageProps'].keys()))
    else:
        print("NEXT DATA not found")
except Exception as e:
    print("HTTP error:", e)
