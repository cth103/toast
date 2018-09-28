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

with open('28-09-2018.log', 'r') as f:
    for line in f:
        s = line.strip().split()
        if s[1] == 'Bathroom':
            if s[2] == 'hum':
                hum.append((calendar.timegm(time.strptime(s[0], '%H:%M:%S')), float(s[3])))
            elif s[2] == 'ref_hum':
                ref_hum.append((calendar.timegm(time.strptime(s[0], '%H:%M:%S')), float(s[3])))
            elif s[2] == 'temp' and s[4] == 'X':
                temp.append((calendar.timegm(time.strptime(s[0], '%H:%M:%S')), float(s[3])))
            elif s[2] == 'ref_temp':
                ref_temp.append((calendar.timegm(time.strptime(s[0], '%H:%M:%S')), float(s[3])))

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
    pred_hum.append(convert_relative_humidity(ref_hum[x][1], ref_temp[x][1], temp[x][1]))

plt.plot([i[0] for i in hum], [i[1] for i in hum], label='room_hum')
plt.plot([i[0] for i in ref_hum], [i[1] for i in ref_hum], label='att hum')
plt.plot([i[0] for i in temp], [i[1] for i in temp], label='room temp')
plt.plot([i[0] for i in ref_temp], [i[1] for i in ref_temp], label='att temp')
plt.plot([i[0] for i in temp], pred_hum, label='pred room')
plt.legend()
plt.show()
