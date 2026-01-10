inline def max(x: Int, y: Int): Int = inline if x > y then x else y

// usage: max(3, 5)
// will be expanded to: if 3 > 5 then 3 else 5
// after constant folding: 5
