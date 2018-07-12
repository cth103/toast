#include "ets_sys.h"
#include "osapi.h"
#include "driver/gpiolib.h"

#include "driver/dhtxx_lib.h"

static int dhtxx_gpio;

typedef enum {
    dhtxx_standby=0,
    dhtxx_mark,
    dhtxx_connecting,
    dhtxx_mark_connecting,
    dhtxx_waiting_bit,
    dhtxx_mark_bit,
    dhtxx_read_bit
} dhtxx_status;
static volatile dhtxx_status  sStatus;

static volatile uint8_t sRead[5];
static volatile uint32_t last_timer;
static ETSTimer dhtxx_timer;

static uint32_t dhtxx_user_task;
static uint32_t dhtxx_user_task_signal;

//-----------------------------------------------------------------------------------------
LOCAL bool ICACHE_FLASH_ATTR
dhtxx_set_read_nok(int code)
{
    int i;
    for(i=0;i<4;i++)
        sRead[i]=0;
    sRead[4] = code;
}

//-----------------------------------------------------------------------------------------
LOCAL void ICACHE_FLASH_ATTR
dhtxx_set_standby()
{
    os_timer_disarm(&dhtxx_timer);
    sStatus = dhtxx_standby;
    os_timer_disarm(&dhtxx_timer);
// Disable interrupt
    gpio_pin_intr_state_set(GPIO_ID_PIN(dhtxx_gpio), GPIO_PIN_INTR_DISABLE);
// GPIO as Output to high level by default.
    GPIO_OUTPUT_SET(dhtxx_gpio,1);
// Read is finish. Signal to aplication
    system_os_post(dhtxx_user_task, dhtxx_user_task_signal, 0);
}

//-----------------------------------------------------------------------------------------
LOCAL void ICACHE_FLASH_ATTR
dhtxx_protocol(uint32 gpio_status, int cause)
{
    static int actual_bit;

    switch(cause) // 0 = gpio interrupt, 1=timer
    {
        case 0: // gpio edge
        {
// disable interrupt for GPIO
            gpio_pin_intr_state_set(GPIO_ID_PIN(dhtxx_gpio), GPIO_PIN_INTR_DISABLE);
// clear interrupt status for GPIO
            GPIO_REG_WRITE(GPIO_STATUS_W1TC_ADDRESS, gpio_status & GPIO_Pin(dhtxx_gpio));
// Reactivate interrupts for GPIO0
            gpio_pin_intr_state_set(GPIO_ID_PIN(dhtxx_gpio), GPIO_PIN_INTR_ANYEDGE);

            switch(sStatus)
            {
                case dhtxx_connecting:
                    if(GPIO_INPUT_GET(dhtxx_gpio))
                    {
// Rising edge ?? Error.
                        dhtxx_set_read_nok(1);
                        dhtxx_set_standby();
                    }
                    else
                    {
                        sStatus = dhtxx_mark_connecting;
                    }
                break;
                case dhtxx_mark_connecting:
                    if(!GPIO_INPUT_GET(dhtxx_gpio))
                    {
// Falling edge ?? Error.
                        dhtxx_set_read_nok(2);
                        dhtxx_set_standby();
                    }
                    else
                    {
                        sStatus = dhtxx_waiting_bit;
                    }
                break;
                case dhtxx_waiting_bit:
                    if(GPIO_INPUT_GET(dhtxx_gpio))
                    {
// Rising edge ?? Error.
                        dhtxx_set_read_nok(3);
                        dhtxx_set_standby();
                    }
                    else
                    {
                        sStatus = dhtxx_mark_bit;
                        actual_bit=0;
                    }
                break;
                case dhtxx_mark_bit:
                    if(! GPIO_INPUT_GET(dhtxx_gpio))
                    {
// Falling edge ?? Error.
                        dhtxx_set_read_nok(4);
                        dhtxx_set_standby();
                    }
                    else
                    {
                        if(actual_bit >= 40)
                        {
                            dhtxx_set_standby();     // finish OK
                        }
                        else
                        {
                            last_timer = system_get_time();
                            sStatus = dhtxx_read_bit;
                        }
                    }
                break;
                case dhtxx_read_bit:
                    if(GPIO_INPUT_GET(dhtxx_gpio))
                    {
// Rising edge ?? Error.
                        dhtxx_set_read_nok(5);
                        dhtxx_set_standby();
                    }
                    else
                    {
// 26-28 uS means 0.   70 uS means 1
                        int bit_data = ((system_get_time()-last_timer) > 40) ? 1:0;
                        int actual_byte = actual_bit / 8;
                        sRead[actual_byte] <<= 1;
                        sRead[actual_byte] |= bit_data;
                        actual_bit++;
                        sStatus = dhtxx_mark_bit;
                    }
                break;
                case dhtxx_standby:
                case dhtxx_mark:
                default:
                    dhtxx_set_standby();
                break;
            }
        }
        break;
        case 1: //timer
            switch(sStatus)
            {
                case dhtxx_mark: // end of mark
                    sStatus = dhtxx_connecting;
                    // GPIO as Output to high level by default.
                    GPIO_OUTPUT_SET(dhtxx_gpio,1);
                    GPIO_AS_INPUT(dhtxx_gpio);

                    ETS_GPIO_INTR_DISABLE();

                    gpio_register_set(GPIO_PIN_ADDR(dhtxx_gpio),
                                       GPIO_PIN_INT_TYPE_SET(GPIO_PIN_INTR_DISABLE)  |
                                       GPIO_PIN_PAD_DRIVER_SET(GPIO_PAD_DRIVER_DISABLE) |
                                       GPIO_PIN_SOURCE_SET(GPIO_AS_PIN_SOURCE));

                    GPIO_REG_WRITE(GPIO_STATUS_W1TC_ADDRESS, BIT(dhtxx_gpio));

                    gpio_pin_intr_state_set(GPIO_ID_PIN(dhtxx_gpio), GPIO_PIN_INTR_ANYEDGE);

                    ETS_GPIO_INTR_ENABLE();

                    os_timer_disarm(&dhtxx_timer);
                    os_timer_arm(&dhtxx_timer,16,0); // wait a "long time" (16ms) before we give up
                break;
                case dhtxx_connecting:
                case dhtxx_mark_connecting:
                case dhtxx_waiting_bit:
                case dhtxx_mark_bit:
                case dhtxx_read_bit:
                default:
                    dhtxx_set_read_nok(128 + sStatus);
                    dhtxx_set_standby();
                break;
            }

        default:
        break;
    }
}

//-----------------------------------------------------------------------------------------
LOCAL void ICACHE_FLASH_ATTR
dhtxx_timer_handler()
{
	dhtxx_protocol(0, 1);
}

//-----------------------------------------------------------------------------------------
void ICACHE_FLASH_ATTR
dhtxx_gpio_intr_handler(uint32 gpio_status)
{
	dhtxx_protocol(gpio_status, 0);
}

//-----------------------------------------------------------------------------------------
void ICACHE_FLASH_ATTR
dhtxx_init(int gpio_id,uint32_t user_task,uint32_t user_task_signal)
{
    dhtxx_gpio = gpio_id;
    dhtxx_user_task = user_task;
    dhtxx_user_task_signal = user_task_signal;

    gpio_config(dhtxx_gpio,GPIO_Mode_Out_OD,GPIO_Pull_DIS,GPIO_PIN_INTR_DISABLE);

// GPIO as Output to high level by default.
    GPIO_OUTPUT_SET(dhtxx_gpio,1);

// Set gpio status. we don't want interrupts now
   gpio_pin_intr_state_set(GPIO_ID_PIN(dhtxx_gpio), GPIO_PIN_INTR_DISABLE);

// disarm and setup timer
    os_timer_disarm(&dhtxx_timer);
    os_timer_setfn(&dhtxx_timer,dhtxx_timer_handler,NULL);
}

//-----------------------------------------------------------------------------------------
void ICACHE_FLASH_ATTR
dhtxx_start_read(void* arg)
{

// set gpio to  0 for 18 mS minimun
    GPIO_OUTPUT_SET(dhtxx_gpio, 0);

    sStatus = dhtxx_mark;

    os_timer_disarm(&dhtxx_timer);
    os_timer_arm(&dhtxx_timer,20,arg);
}

//-----------------------------------------------------------------------------------------
bool ICACHE_FLASH_ATTR
dhtxx_error()
{
    int i;
    uint8_t Result = 0;
    for (i = 0; i < 4;i++) {
	    Result += sRead[i];
    }
    if (Result != sRead[4]) {
	    /* there's an error; the last number given to dhtxx_set_read_nok will be here */
	    return sRead[4];
    }
    /* OK */
    return 0;
}

int ICACHE_FLASH_ATTR
dht11_get_temperature()
{
    return(sRead[2]*1000+sRead[3]);
}

int ICACHE_FLASH_ATTR
dht11_get_rh()
{
    return(sRead[0]*1000+sRead[1]);
}

int ICACHE_FLASH_ATTR
dht22_get_temperature()
{
    return(sRead[2]*256+sRead[3]);
}

int ICACHE_FLASH_ATTR
dht22_get_rh()
{
    return(sRead[0]*256+sRead[1]);
}
