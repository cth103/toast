import matplotlib
matplotlib.use('GTK3Cairo')
import matplotlib.pyplot as plt
import numpy as np
import time
import calendar
from scipy.signal import butter, filtfilt, savgol_filter
from scipy.interpolate import UnivariateSpline

hum = []

def moving_average(a, n=3):
    ret = np.cumsum(a, dtype=float)
    ret[n:] = ret[n:] - ret[:-n]
    return ret[n - 1:] / n

with open('19-09-2018.log', 'r') as f:
    for line in f:
        s = line.strip().split()
        if s[1] != 'Bathroom' or s[2] != 'hum':
            continue
        hum.append((calendar.timegm(time.strptime(s[0], '%H:%M:%S')), float(s[3])))

class HumidityProcessor(object):
    def __init__(self, maf_len, rising, falling):
        self.maf_len = maf_len
        self.rising = rising
        self.falling = falling
        self.fan_on = False
        self.maf = []
        self.last = None
        self.base = None

    def add(self, v):
        self.maf.append(v)
        if len(self.maf) <= self.maf_len:
            return self.fan_on
        del self.maf[0]
        v = sum(self.maf) / self.maf_len
        if self.last is not None:
            print v - self.last
        if self.last is not None and not self.fan_on and (v - self.last) > self.rising:
            self.fan_on = True
            self.base = self.maf[0]
        elif self.fan_on and (v - self.base) < self.falling:
            self.fan_on = False
        self.last = v
        return self.fan_on
            

humproc = HumidityProcessor(5, 4, 0)
fan = []
for h in hum:
    fan_on = humproc.add(h[1])
    if fan_on:
        fan.append(100)
    else:
        fan.append(0)

#smoothed_hum = savgol_filter(hum_value, 501, 2)
#smoothed_hum = moving_average(hum_value, 10)
#N = len(smoothed_hum)
#plt.plot(hum_time[0:N], smoothed_hum)

#fan_on = []
#for i in range(0, N):
#    if hum_value[i] > (smoothed_hum[i] + 10):
#        fan_on.append(100)
#    else:
#        fan_on.append(0)

plt.plot([i[0] for i in hum], fan)
plt.plot([i[0] for i in hum], [i[1] for i in hum])
#plt.plot([i[0] for i in hum], smoothed)
plt.show()
