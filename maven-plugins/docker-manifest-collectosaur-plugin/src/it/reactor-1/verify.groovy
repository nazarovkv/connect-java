import groovy.json.*

def json = new JsonSlurper().parseText(new File("target/it/reactor-1/target/connect-manifest.json").text)

assert json.size() == 3
assert json.find({it.baseImageName == 'module-3' && it.fullImageName == 'blah/module-3'})
assert json.find({it.baseImageName == 'module-2' && it.fullImageName == 'blah/module-2'})
assert json.find({it.baseImageName == 'module-1' && it.fullImageName == 'blah/module-1'})