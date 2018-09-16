
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

def convert_relative_humidity(from_hum, from_temp, to_temp):
    """Taken from https://www.vaisala.com/sites/default/files/documents/Humidity_Conversion_Formulas_B210973EN-F.pdf"""

    # Magic constants
    A = 6.116441
    m = 7.591386
    T_n = 240.2763
    C = 2.16679
    Az = 273.15

    P_w = A * (10 ** ((m * from_temp) / (from_temp + T_n))) * from_hum / 100
    abs_hum = C * (P_w * 100) / (Az + from_temp)

    P_w2 = (Az + to_temp) * abs_hum / C
    return P_w2 / (A * (10 ** ((m * to_temp) / (to_temp + T_n))))
