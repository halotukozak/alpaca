def replaceWithNewCtx(newCtx: Term) = new ReplaceRefs[quotes.type].apply(
  (find = oldCtx.symbol, replace = newCtx),
  (find = tree.symbol, replace = Select.unique(newCtx, "lastRawMatched")),
)
