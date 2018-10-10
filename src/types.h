#define OP_SEND_BASIC 0x00
#define OP_SEND_ALL   0x01
#define OP_CHANGE     0x80

#define OP_TEMPERATURES (OP_CHANGE | 0x2)
#define OP_HUMIDITIES   (OP_CHANGE | 0x4)
#define OP_RULES        (OP_CHANGE | 0x8)
#define OP_ACTUATORS    (OP_CHANGE | 0x10)
#define OP_PERIODS      (OP_CHANGE | 0x20)
#define OP_ALL          (OP_CHANGE | 0x3c)
