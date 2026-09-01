package halotukozak
package alpaca.internal

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.{Files, Path}
import java.util.concurrent.ConcurrentHashMap

private[internal] object logger:
  // `content` is by-name so the (potentially expensive) Showable rendering at the call site is
  // only ever forced inside the `foreach` below, i.e. when `ALPACA_DEBUG_DIR` is actually set --
  // otherwise it would run unconditionally on every macro expansion, debug directory or not.
  // noinspection AccessorLikeMethodIsUnit
  inline def toFile(path: String, replace: Boolean)(content: => Shown)(using debugSettings: DebugSettings): Unit =
    debugSettings.debugDirectory.foreach: dir =>
      val file = Path.of(dir).resolve(path)
      if replace then this.replace(file)(content) else this.append(file)(content)

  private val writerCache = new ConcurrentHashMap[Path, BufferedWriter]

  def append(path: Path)(content: Shown): Unit = writerCache.compute(
    path,
    (p, existing) => (if existing == null then createWriter(p, false) else existing).tap(_.write(content)),
  )

  def replace(path: Path)(content: Shown): Unit = writerCache
    .compute(
      path,
      (p, existing) =>
        if existing != null then existing.close()
        createWriter(p, true).tap(_.write(content)),
    )

  private def createWriter(path: Path, replace: Boolean): BufferedWriter =
    if path.getParent != null then Files.createDirectories(path.getParent)
    new BufferedWriter(new FileWriter(path.toFile, !replace))
