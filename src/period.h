#include <stdint.h>
#include <string>

class Period
{
public:
	Period(uint8_t*& p);

	std::string zone() const {
		return _zone;
	}

	float target() const {
		return _target;
	}

	time_t from() const {
		return _from;
	}

	time_t to() const {
		return _to;
	}

	void get(uint8_t*& p) const;

private:
	std::string _zone;
	float _target;
	time_t _from;
	time_t _to;
};
