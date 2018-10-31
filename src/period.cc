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
Period::get(uint8_t*& p, uint8_t* e) const
{
	put_string(p, e, _zone);
	put_float(p, e, _target);
	put_int64(p, e, _from);
	put_int64(p, e, _to);
}
