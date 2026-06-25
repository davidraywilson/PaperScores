import requests
import json
import xml.etree.ElementTree as ET

url = "https://apigw.fotmob.com/matches?date=20260623"
r = requests.get(url)
xml_data = r.text

# find first match
try:
    start = xml_data.index('<match ')
    end = xml_data.index('/>', start)
    match_str = xml_data[start:end+2]
    print(match_str)
    
    # extract id
    import re
    m = re.search(r'id="([^"]+)"', match_str)
    if m:
        match_id = m.group(1)
        print("Found match_id:", match_id)
        
        # Test api endpoint
        api_url = f"https://www.fotmob.com/api/matchDetails?matchId={match_id}"
        print("Testing", api_url)
        r2 = requests.get(api_url, headers={"User-Agent": "Mozilla/5.0"})
        print(r2.status_code)
        print(r2.text[:200])
except Exception as e:
    print(e)
