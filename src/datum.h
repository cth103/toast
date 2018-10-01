#ifndef TOAST_DATUM_H
#define TOAST_DATUM_H

#include <time.h>

class Datum
{
public:
	explicit Datum(float value);

	time_t time() const {
		return _time;
	}

	float value() const {
		return _value;
	}

private:
	time_t _time;
	float _value;
};

#endif
