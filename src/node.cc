#include "node.h"
#include <iostream>

using std::list;
using std::shared_ptr;
using std::scoped_lock;
using std::mutex;
using std::cout;

list<shared_ptr<Node>> Node::_all;
mutex Node::_all_mutex;

list<shared_ptr<Node>>
Node::all()
{
	scoped_lock lm(_all_mutex);
	return _all;
}

void
Node::add(shared_ptr<Node> node)
{
	scoped_lock lm(_all_mutex);
	cout << node->name() << " is here.\n";
	_all.push_back(node);
}
