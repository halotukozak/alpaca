// during macro expansion:
val oldCtxSymbol: Symbol = ... // macro parameter symbol
val newCtxRef: Term = Ref(newCtxSymbol) // reference to new symbol

val replaceRefs = ReplaceRefs()
val treeMap = replaceRefs((oldCtxSymbol, newCtxRef))

// during function body transformation:
val originalBody: Term = ... // function body that references oldCtx
val transformedBody: Term = treeMap.transformTree(originalBody)(owner)
// all occurrences of oldCtxSymbol are now replaced with newCtxRef
