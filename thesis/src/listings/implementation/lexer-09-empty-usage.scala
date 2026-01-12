case class Config(
  name: String = "default",
  count: Int = 0,
)
// compiler automatically derives Empty[Config]
val empty = summon[Empty[Config]]
val instance: Config = empty() // Config("default", 0)
