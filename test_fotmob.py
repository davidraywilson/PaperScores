import requests

def get_match_details(match_id):
    url = f"https://www.fotmob.com/api/matchDetails?matchId={match_id}"
    response = requests.get(url)
    
    if response.status_code == 200:
        print("Success JSON!")
    else:
        print(f"Failed to retrieve data: {response.status_code}")
        print(response.text[:200])

get_match_details("5225665")
