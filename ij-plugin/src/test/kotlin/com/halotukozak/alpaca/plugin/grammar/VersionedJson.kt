package com.halotukozak.alpaca.plugin.grammar

/** Wraps [json] in the `{"version": ..., "context": ...}` envelope every real compile-time export
 *  file is written in, for tests that hand-write fixture JSON instead of using a real export. */
fun versionedJson(
    json: String,
    version: Int = CURRENT_EXPORT_FORMAT_VERSION,
): String = """{"version":$version,"context":$json}"""
