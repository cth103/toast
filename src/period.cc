#include "period.h"
#include "util.h"
#include <iostream>

using std::cout;

Period::Period(uint8_t*& p)
{
	_zone = get_string(p);
	_target = get_float(p);
	_from = get_int64(p);
	_to = get_int64(p);
}

void
Period::get(uint8_t*& p) const
{
	put_string(p, _zone);
	put_float(p, _target);
	put_int64(p, _from);
	put_int64(p, _to);
}
