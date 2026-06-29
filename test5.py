import urllib.request
import re

req = urllib.request.Request('https://www.fotmob.com/matches?date=20240714', headers={'User-Agent': 'Mozilla/5.0'})
try:
    html = urllib.request.urlopen(req).read().decode('utf-8')
    # Find all /match/<id> links
    matches = re.findall(r'href="/match/(\d+)/', html)
    print("Found match IDs:", set(matches))
except Exception as e:
    print("HTTP error:", e)
