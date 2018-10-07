#include "../src/config.h"
#include <boost/test/unit_test.hpp>

BOOST_AUTO_TEST_CASE(config_test)
{
	Config c("test/config1");
	BOOST_REQUIRE_EQUAL(c.log_types(), 9);
	BOOST_REQUIRE_EQUAL(c.server_port(), 1234);
	BOOST_REQUIRE_EQUAL(c.log_directory(), "/dev/null");
	BOOST_REQUIRE_EQUAL(c.hidden_zones().size(), 1);
	BOOST_REQUIRE_EQUAL(c.hidden_zones().front(), "Landing");
}
