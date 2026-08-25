rootProject.name = "request-flow-visualizer"

include("core")
include("starter-boot3")
include("starter-boot2")
include("sample-app")

project(":core").name = "request-flow-visualizer-core"
project(":starter-boot3").name = "request-flow-visualizer-spring-boot-starter"
project(":starter-boot2").name = "request-flow-visualizer-spring-boot2-starter"
