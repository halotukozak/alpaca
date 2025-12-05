// Podczas ekspansji makra:
val oldCtxSymbol: Symbol = ... // Symbol parametru makra
val newCtxRef: Term = Ref(newCtxSymbol) // Referencja do nowego symbolu

val replaceRefs = ReplaceRefs()
val treeMap = replaceRefs((oldCtxSymbol, newCtxRef))

// Transformacja ciała funkcji:
val originalBody: Term = ... // Ciało funkcji odnoszące się do oldCtx
val transformedBody: Term = treeMap.transformTree(originalBody)(owner)
// Wszystkie wystąpienia oldCtxSymbol są teraz zastąpione newCtxRef
