#include <avr/io.h>
#include <util/delay.h>

int main()
{
	DDRB |= _BV(DDB1);
	while (1) {
		PORTB |= _BV(PORTB1);
		_delay_ms(50);
		PORTB &= ~_BV(PORTB1);
		_delay_ms(50);
	}

	return 0;
}
