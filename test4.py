import urllib.request
import re
import json

req = urllib.request.Request('https://www.fotmob.com/', headers={'User-Agent': 'Mozilla/5.0'})
try:
    html = urllib.request.urlopen(req).read().decode('utf-8')
    m = re.search(r'__NEXT_DATA__" type="application/json">([^<]+)</script>', html)
    if m:
        data = json.loads(m.group(1))
        # Find any match ID
        match_ids = set()
        def find_ids(obj):
            if isinstance(obj, dict):
                if 'id' in obj and isinstance(obj['id'], (int, str)):
                    # heuristically guess if it's a match ID (usually 7 digits)
                    try:
                        if len(str(obj['id'])) >= 6 and str(obj['id']).isdigit():
                            match_ids.add(str(obj['id']))
                    except: pass
                for k, v in obj.items():
                    find_ids(v)
            elif isinstance(obj, list):
                for item in obj:
                    find_ids(item)
                    
        find_ids(data)
        print("Found some IDs:", list(match_ids)[:10])
except Exception as e:
    print("HTTP error:", e)
