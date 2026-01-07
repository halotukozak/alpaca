case class User(
  name: String,
  age: Int,
) 

// compiler automatically derives Copyable[User]
// val copy = summon[Copyable[User]]
// val user = User("Alice", 30)
// val copied: User = copy(user) // User("Alice", 30)
