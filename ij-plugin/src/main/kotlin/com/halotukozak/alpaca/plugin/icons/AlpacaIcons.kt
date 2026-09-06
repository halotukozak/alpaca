package com.halotukozak.alpaca.plugin.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/** Shared icons for the plugin, loaded once from `icons/alpaca.svg` (see `META-INF/pluginIcon.svg`
 *  for the separate, more detailed Plugin Logo shown in the Marketplace/Settings). */
object AlpacaIcons {
    /** Used for every dynamically registered [com.halotukozak.alpaca.plugin.lexer.AlpacaFileType]
     *  and every Structure View node: the plugin is grammar-agnostic, so there's no per-grammar or
     *  per-nonterminal icon to pick a more specific one from. */
    val FILE: Icon = IconLoader.getIcon("/icons/alpaca.svg", AlpacaIcons::class.java.classLoader)
}
