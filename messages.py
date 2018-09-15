
OP_SEND_BASIC                   = 0
OP_SEND_ALL                     = 1

OP_CHANGE                       = 0x10
OP_HEATING_ENABLED              = OP_CHANGE | 0
OP_ZONE_HEATING_ENABLED         = OP_CHANGE | 1
OP_TARGET                       = OP_CHANGE | 2
OP_BOILER_ON                    = OP_CHANGE | 3
OP_TEMPERATURES                 = OP_CHANGE | 4
OP_HUMIDITIES                   = OP_CHANGE | 5
OP_RULES                        = OP_CHANGE | 6
OP_OUTSIDE_HUMIDITIES           = OP_CHANGE | 7
OP_OUTSIDE_TEMPERATURES         = OP_CHANGE | 8
OP_ALL                          = OP_CHANGE | 0xf
