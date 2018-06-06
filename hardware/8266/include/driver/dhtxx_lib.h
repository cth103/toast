#ifndef DHTXX_LIB_H
#define DHTXX_LIB_H

// DHTXX is a low cost Temperature and Relative Humidity sensor.
// It use a one wire protocol to send data to MCU (ESP8266 in this case)
//
// Your application must:
//   - Indicate with dhtxx_init()
//        - What gpio use to communicate with
//        - What task get signaled when read will be finished
//        - What signal send
//   - Setup a gpio interrupt handler.
//     When a interrupt is receive from gpio selected, the gpio interrupt
//     handler must execute, dhtxx_gpio_interrupt_handler() function.
//   - Start a read executing dhtxx_start_read() function.
//   - Wait to end read is signaled by dhtxx library
//   - Verify read was correct using dhtxx_error() function.
//   - Read temperature and RH with dhtxx_get_temperature() and
//     dhtxx_get_rh().
//


// dhtxx_init :
// Initialize communication protocol with dhtxx
// Params:
//   gpio_id: GPIO ID to communicate with DHTXX
//   user_task: user_task priority to signal read end
//   user_task_signal : The signal will be sent to user_task when read is finish
void dhtxx_init(int gpio_id,uint32_t user_task,uint32_t user_task_signal);


// dhtll_start_read()
// Start a DHTXX Read
//
void dhtxx_start_read();

// dhtxx_gpio_intr_handler:
// General GPIO interrupt handler must be execute this function when a
// interrupt from DHTXX gpio selected arrive.
//
void  dhtxx_gpio_intr_handler(uint32 gpio_status);

//
// dhtxx_error :
// Return true if was a error on last comunication with dhtxx
//
bool dhtxx_error();

//
// dhtxx_get_temperature :
// Get last readed temperature (milli ºC)
// The result of this funcion is undefined if dhtxx_error() return true
//
int dhtxx_get_temperature();

//
// dhtxx_get_rh :
// Get last readed relative humidity (in %% (per thousand)
// The result of this funcion is undefined if dhtxx_error() return true
//
int dhtxx_get_rh();


#endif // DHTXX_LIB_H
