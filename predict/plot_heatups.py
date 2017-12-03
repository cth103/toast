import matplotlib.pyplot as plt
import numpy
from scipy.signal import butter, filtfilt
from scipy.interpolate import UnivariateSpline

class Day:
    def __init__(self, log_file):
        times = []
        self.temps = []
        self.ons = []
        with open(log_file, 'r') as f:
            state = 0
            tail = 0
            for line in f:
                s = line.strip().split()
                if state == 0 and bool(int(s[3])):
                    state = 1
                elif state == 1 and not bool(int(s[3])):
                    state = 2

                if state == 1:
                    tail += 2

                if state == 2:
                    tail -= 1
                    if tail == 0:
                        state = 3

                if state == 1 or state == 2:
                    times.append(s[0])
                    self.temps.append(float(s[1]))
                    self.ons.append(bool(int(s[3])))

        self.seconds = []
        first = None
        for t in times:
            s = t.split(':')
            v = int(s[0]) * 3600 + int(s[1]) * 60 + int(s[2])
            if first is None:
                first = v
            self.seconds.append(v - first)

        if len(self.temps) > 0:
            x = numpy.linspace(0, len(self.temps), len(self.temps))
            spl = UnivariateSpline(x, self.temps)
            spl.set_smoothing_factor(0.75)
            self.smoothed_temps = spl(x)
        else:
            self.smoothed_temps = []

    def period(self):
        if len(self.seconds) == 0:
            return 0
        return self.seconds[-1]

    def plot(self, offset):
#        plt.plot(numpy.array(self.seconds) + offset, self.smoothed_temps)
        plt.plot(numpy.array(self.seconds) + offset, numpy.array(self.ons) * 5 + 15)
        if len(self.smoothed_temps) > 0:
            plt.plot(numpy.array(self.seconds) + offset, numpy.gradient(self.smoothed_temps) + 20)

days = []
for i in range(5, 15):
    days.append(Day('logs/%02d-01-2015.log' % i))

longest = None
for d in days:
    if longest is None or d.period() > longest:
        longest = d.period()

for d in days:
    d.plot((longest - d.period()) / 3)

plt.show()
