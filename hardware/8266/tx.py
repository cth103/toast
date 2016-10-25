import serial

def send(s, t):
    print t
    s.write(t)

def ok(s):
    d = ""
    while True:
        x = s.read(size=1)
        if x != '':
            d += x
        if len(d) >= 2 and d[-2] == '\r' and d[-1] == '\n':
            if d == 'OK\r\n':
                return
            print d
            d = ""

s = serial.Serial('/dev/ttyUSB0', 9600, timeout=1)
send(s, 'AT\r\n')
ok(s)
send(s, 'ATE0\r\n')
ok(s)
send(s, 'AT+CWJAP="PlusnetWireless998FC7","E5383F2817"\r\n')
ok(s)
send(s, 'AT+CIFSR\r\n')
ok(s)
send(s, 'AT+CIPSTART="TCP","192.168.1.1",4024\r\n')
ok(s)
send(s, 'AT+CIPSEND=8\r\n')
send(s, 'FUCKFUCKARSEBOLLOCKS+++')
print s.read(size=128)
#ok(s)
