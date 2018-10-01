def options(opt):
    opt.load('compiler_cxx')

def configure(conf):
    conf.load('compiler_cxx')
    conf.env.append_value('CXXFLAGS', ['-Wall', '-Wextra', '-std=c++17'])
    conf.check(lib=['boost_system', 'pthread'], uselib_store='BOOST_SYSTEM', msg="Checking for library boost-system")

def build(bld):
    bld.recurse('src')
