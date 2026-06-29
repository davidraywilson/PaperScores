import urllib.request
import re
import json

req = urllib.request.Request('https://www.fotmob.com/leagues/42/champions-league', headers={'User-Agent': 'Mozilla/5.0'})
try:
    html = urllib.request.urlopen(req).read().decode('utf-8')
    m = re.search(r'__NEXT_DATA__" type="application/json">([^<]+)</script>', html)
    if m:
        data = json.loads(m.group(1))
        # Find any match ID
        match_ids = set()
        def find_ids(obj):
            if isinstance(obj, dict):
                if 'id' in obj and isinstance(obj['id'], (int, str)) and str(obj['id']).isdigit() and len(str(obj['id'])) > 5:
                    match_ids.add(str(obj['id']))
                for k, v in obj.items():
                    find_ids(v)
            elif isinstance(obj, list):
                for item in obj:
                    find_ids(item)
                    
        find_ids(data['props']['pageProps'])
        print("Found some IDs:", list(match_ids)[:10])
except Exception as e:
    print("HTTP error:", e)
