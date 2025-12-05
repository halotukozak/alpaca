'{
    Map(
      (0, Terminal("PLUS")) -> Shift(1),
      (0, Terminal("NUMBER")) -> Shift(2),
      ...
      (999, Terminal("EOF")) -> Reduction(prod),
    )
  }
