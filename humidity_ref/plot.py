import matplotlib
matplotlib.use('GTK3Cairo')
import matplotlib.pyplot as plt
import numpy as np
import time
import calendar
from scipy.signal import butter, filtfilt, savgol_filter
from scipy.interpolate import UnivariateSpline

hum = []
ref_hum = []
temp = []
ref_temp = []

def moving_average(a, n=3):
    ret = np.cumsum(a, dtype=float)
    ret[n:] = ret[n:] - ret[:-n]
    return ret[n - 1:] / n

with open('11-10-2018.log', 'r') as f:
    for line in f:
        s = line.strip().split()
        if s[1] == 'loft' and s[2] == 'humidity':
            if s[3] == 'Bathroom':
                hum.append((calendar.timegm(time.strptime(s[0], '%H:%M:%S')), float(s[4])))
            elif s[3] == 'Landing':
                ref_hum.append((calendar.timegm(time.strptime(s[0], '%H:%M:%S')), float(s[4])))

def convert_relative_humidity(from_hum, from_temp, to_temp):
    """Taken from https://www.vaisala.com/sites/default/files/documents/Humidity_Conversion_Formulas_B210973EN-F.pdf"""

    print 'hum is %f @ temp %f  ->  temp %f' % (from_hum, from_temp, to_temp)
    # Magic constants
    A = 6.116441
    m = 7.591386
    T_n = 240.2763
    C = 2.16679
    Az = 273.15

    P_w = A * (10 ** ((m * from_temp) / (from_temp + T_n))) * from_hum / 100
    abs_hum = C * (P_w * 100) / (Az + from_temp)

    P_w2 = (Az + to_temp) * abs_hum / C
    R = P_w2 / (A * (10 ** ((m * to_temp) / (to_temp + T_n))))
    print R
    return R

pred_hum = []
for x in range(0, len(temp)):
    print len(ref_hum), len(ref_temp), len(temp)
    pred_hum.append(convert_relative_humidity(ref_hum[x][1], ref_temp[x][1], temp[x][1]))

hum_diff = []
for x in range(0, len(ref_hum)):
    hum_diff.append(hum[x][1] - ref_hum[x][1])

print len(hum_diff)

on = []
curr = False
up = 8
down = 5
for x in hum_diff:
    if not curr and x > up:
        curr = True
    elif curr and x < down:
        curr = False
    on.append(curr * 25)

#plt.plot([i[0] for i in hum], [i[1] for i in hum], label='room_hum')
#plt.plot([i[0] for i in ref_hum], [i[1] for i in ref_hum], label='ref hum')
#plt.plot([i[0] for i in temp], [i[1] for i in temp], label='room temp')
#plt.plot([i[0] for i in ref_temp], [i[1] for i in ref_temp], label='ref temp')
#plt.plot([i[0] for i in temp], pred_hum, label='pred room')
plt.plot([i[0] for i in hum], hum_diff, label='humidity diff')
#plt.plot([i[0] for i in temp], on, label='fan on')
plt.legend()
plt.show()
