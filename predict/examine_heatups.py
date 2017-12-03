import matplotlib.pyplot as plt
import numpy
from scipy.signal import butter, filtfilt
from scipy.interpolate import UnivariateSpline
import os

current = None
tailing = False
day = 1
month = 1
day_number = 0

def seconds(stamp):
    s = stamp.split(':')
    return int(s[0]) * 3600 + int(s[1]) * 60 + int(s[2])

def path(day, month):
    return 'logs/%02d-%02d-2015.log' % (day, month)

class Heat:
    def __init__(self, time, temp):
        self.on_time = time
        self.on_temp = temp
        self.tail_times = []
        self.tail_temps = []

    def dT(self):
        return self.off_temp - self.on_temp

    def dt(self):
        return self.off_time - self.on_time

    def c_per_s(self):
        return self.dT() / self.dt()
        return

    def smoothed_tail_temps(self):
        x = numpy.linspace(0, len(self.tail_temps), len(self.tail_temps))
        spl = UnivariateSpline(x, self.tail_temps)
        spl.set_smoothing_factor(0.75)
        return spl(x)

    def overshoot(self):
        if len(self.tail_temps) == 0:
            return 0

        return max(0, max(self.tail_temps) - self.off_temp)

    def __str__(self):
        return 'dT = %f, dt = %d, C/s = %f' % (self.dT(), self.dt(), self.c_per_s())

heats = []

while True:
    with open(path(day, month), 'r') as f:
        for line in f:
            s = line.strip().split()
            this = bool(int(s[3]))
            time = day_number * 86400 + seconds(s[0])
            temp = float(s[1])
            if (current is None or tailing) and this == True:
                # on
                tailing = False
                current = Heat(time, temp)
                heats.append(current)
            elif (current is not None and tailing == False) and this == False:
                # off
                current.off_time = time
                current.off_temp = temp
                tailing = True
            elif tailing and this == False:
                current.tail_times.append(time)
                current.tail_temps.append(temp)

    day_number += 1
    day += 1
    if not os.path.exists(path(day, month)):
        day = 1
        month += 1
    if not os.path.exists(path(day, month)):
        break

c_per_s = []
for h in heats:
    c_per_s.append(h.c_per_s())

#plt.hist(c_per_s, bins=100)
#plt.show()

overshoots = []
c_per_ss = []

for h in heats:
    if h.overshoot() > 0:
        overshoots.append(h.overshoot())
        c_per_ss.append(h.c_per_s())

plt.scatter(c_per_ss, overshoots)

# for h in heats[0:20]:

#     if h.overshoot() == 0:
#         continue

#     x = []
#     y = []
#     if len(h.tail_temps) == 0:
#         continue
#     temps = h.smoothed_tail_temps()
#     for i in range(0, len(h.tail_times)):
#         x.append(h.tail_times[i] - h.tail_times[0])
#         y.append(temps[i])

#     plt.plot(x, y)

plt.show()
