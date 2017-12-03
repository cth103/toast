import matplotlib.pyplot as plt
import numpy
from scipy.signal import butter, filtfilt
from scipy.interpolate import UnivariateSpline

time = []
temp = []
target = []
on = []
enabled = []

with open('17-01-2015.log', 'r') as f:
    for line in f:
        s = line.strip().split()
        time.append(s[0])
        temp.append(float(s[1]))
        target.append(float(s[2]))
        on.append(bool(int(s[3])))
        enabled.append(bool(int(s[4])))

N = len(temp)

x = numpy.linspace(0, N, N)
spl = UnivariateSpline(x, temp)
spl.set_smoothing_factor(0.75)
xi = numpy.linspace(0, N, N * 4)

smoothed = spl(xi)
gradient = numpy.gradient(smoothed)
gradient2 = numpy.gradient(gradient)

# M = len(stemp)
# dtemp = numpy.zeros(M)
# dtemp[0] = (stemp[2] + 4 * stemp[1] - 3 * stemp[0]) / 2
# for i in range(1, M - 1):
#     dtemp[i] = (stemp[i + 1] - stemp[i - 1]) / 2
# dtemp[M - 1] = (stemp[M - 3] - 4 * stemp[M - 2] + 3 * stemp[M - 1]) / 2

# dtemp[0] = 0

tp = numpy.zeros(len(smoothed))
for i in range(1, len(gradient)):
    tp[i] = (gradient[i] > 0 and gradient[i - 1] < 0) or (gradient[i] < 0 and gradient[i - 1] > 0)

#plt.plot(temp)
plt.plot(xi, smoothed, label='smoothed temp')
plt.plot(xi, gradient + 20, label='dtemp/dt')
plt.plot(xi, gradient2 * 20 + 20, label='d2temp/dt2')
plt.plot(xi, tp * 0.2 + 20, label='turning points')
plt.plot(numpy.array(on) * 0.2 + 20, label='on')
plt.plot(numpy.array(enabled) * 0.2 + 20, label='enabled')
#plt.plot(dtemp + 20)
plt.legend()
plt.show()
