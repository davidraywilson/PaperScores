import urllib.request
import re
import json

class RedirectHandler(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return urllib.request.Request(newurl, headers=req.headers)

opener = urllib.request.build_opener(RedirectHandler())
# Try to fetch matches for June 1, 2024
req = urllib.request.Request('https://www.fotmob.com/api/matches?date=20240601', headers={'User-Agent': 'Mozilla/5.0'})
try:
    html = opener.open(req).read().decode('utf-8')
    data = json.loads(html)
    for l in data.get("leagues", []):
        if l.get("name") == "Champions League":
            for m in l.get("matches", []):
                print(m.get("id"), m.get("home", {}).get("name"), m.get("away", {}).get("name"))
except Exception as e:
    print("HTTP error:", e)
