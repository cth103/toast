#include "datum.h"

int const Datum::size = 8 + 2;

Datum::Datum(float value)
	: _value(value)
{
	_time = ::time(0);
}
