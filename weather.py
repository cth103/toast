
API_KEY = 'd307194c-d901-44a7-8dc9-2d1ce1788047'
LATITUDE = 53.95763
LONGITUDE = -1.08271

import sys
sys.path.append('metoffer')
import datetime
import metoffer

from util import Datum
import time

class Weather(object):
    def __init__(self):
        self.time = None
        self.humidity = None
        self.temperature = None
        self.met = metoffer.MetOffer(API_KEY)

    def fetch(self):
        x = self.met.nearest_loc_obs(LATITUDE, LONGITUDE)
        y = metoffer.Weather(x)
        self.time = y.data[-1]['timestamp'][0]
        self.humidity = y.data[-1]['Screen Relative Humidity'][0]
        self.temperature = y.data[-1]['Temperature'][0]

    def get_humidity(self):
        if self.time is None or (datetime.datetime.now() - self.time).total_seconds() > 3600:
            self.fetch()
        return Datum(self.humidity, self.time.timetuple())

    def get_temperature(self):
        if self.time is None or (datetime.datetime.now() - self.time).total_seconds() > 3600:
            self.fetch()
        return Datum(self.temperature, self.time.timetuple())

state = Weather()
