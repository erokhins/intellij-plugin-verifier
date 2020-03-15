package com.jetbrains.pluginverifier.warnings

/**
 * Whether the IntelliJ plugin can be loaded/unloaded without IDE restart:
 * 1) Plugin can be loaded/unloaded immediately in the IDE Plugins window without pressing the "Apply" button.
 * 2) Plugin can be loaded/unloaded without restart of the IDE but not immediately.
 * 3) Plugin can not be loaded/unloaded without IDE restart.
 *
 * For each of the result types there are reasons provided.
 */
sealed class DynamicPluginStatus {

  abstract val reasonsNotToLoadUnloadWithoutRestart: Set<String>

  object MaybeDynamic : DynamicPluginStatus() {
    override val reasonsNotToLoadUnloadWithoutRestart
      get() = emptySet<String>()
  }

  data class NotDynamic(override val reasonsNotToLoadUnloadWithoutRestart: Set<String>) : DynamicPluginStatus()
}