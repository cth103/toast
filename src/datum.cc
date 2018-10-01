#include "datum.h"

Datum::Datum(float value)
	: _value(value)
{
	_time = time_t(0);
}
