import groovy.json.*

def json = new JsonSlurper().parseText(new File("target/it/reactor-1/target/connect-manifest.json").text)

assert json.size() == 5
assert json.find({it.baseImageName == 'module-3' && it.fullImageName == 'blah/module-3:3.0'})
assert json.find({it.baseImageName == 'module-2' && it.fullImageName == 'blah/module-2:2.0'})
assert json.find({it.baseImageName == 'module-1' && it.fullImageName == 'blah/module-1:1.0'})
assert json.find({it.baseImageName == 'module-golang1' && it.fullImageName == 'blah/golang1-2:2.0' && it.module == 'golang1'})
assert json.find({it.baseImageName == 'module-python1' && it.fullImageName == 'blah/python1-2:7.0' && it.module == 'python1'})
