case class Config(
  name: String = "default",
  count: Int = 0,
)
// Kompilator automatycznie derywuje instancje Empty[Config]
val empty = summon[Empty[Config]]
val instance: Config = empty() // Config("default", 0)
