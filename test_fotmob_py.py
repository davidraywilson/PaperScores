import urllib.request
import json
url = "https://www.fotmob.com/api/matchDetails?matchId=4667807"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)'})
try:
    with urllib.request.urlopen(req) as response:
        print(response.read().decode('utf-8')[:200])
except Exception as e:
    print("Error:", e)
