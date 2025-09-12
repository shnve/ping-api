from faker import Faker
import requests

RATE_LIMIT = 6
API_ENDPOINT = 'http://localhost:8080/api/users'
HEADERS = {
    'Content-Type': 'application/json'
}
PREFERRED_AREA_ID = 'XXXX'

fake = Faker('fr_FR')

def user_creation():
    payload = {
        'title': fake.random_element(['MR', 'MRS']),
        'firstname': fake.first_name(),
        'lastname': fake.last_name(),
        'email': fake.email(),
        'password': fake.password(length=12),
        'phoneNumber': fake.numerify(text='+33%%%%%%%%%'),
        'mobilePhoneNumber': fake.numerify(text='+33%%%%%%%%%'),
        'organization': fake.company(),
        'shipName': fake.word(),
        'homePort': fake.word(),
        'registrationNumber': fake.text(max_nb_chars=10),
        'hasEmailNotification': fake.boolean(),
        'hasAcceptedTos': True,
        'preferredAreaId': PREFERRED_AREA_ID
    }
    print(payload)
    r = requests.post(url=API_ENDPOINT, json=payload, headers=HEADERS)
    print(f"status code: {r.status_code}, result: {r.content}")
    if r.status_code == 429:
        print("RateLimiter 'rateLimiterUserCreateAPI' does not permit further calls")

if __name__ == '__main__':
    for _ in range(RATE_LIMIT):
        user_creation()