package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.ignoring.PluginIgnoredEvent
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportageImpl
import com.jetbrains.pluginverifier.reporting.verification.VerificationReporterSet
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportersProvider
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.verifiers.resolution.DefaultClsResolverProvider
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.Task

/**
 * [Task] verifies the [plugin] [updateInfo]
 * against the [ideVersion] in the [verifierExecutor]
 * using the [JDK] [jdkPath].
 */
class VerifyPluginTask(
    private val verifierExecutor: VerifierExecutor,
    private val updateInfo: UpdateInfo,
    private val ideVersion: IdeVersion,
    private val jdkPath: JdkPath,
    private val pluginDetailsCache: PluginDetailsCache,
    private val ideDescriptorsCache: IdeDescriptorsCache,
    private val jdkDescriptorsCache: JdkDescriptorsCache,
    private val pluginRepository: PluginRepository
) : Task<VerificationResult>("Check $updateInfo against $ideVersion"), Comparable<VerifyPluginTask> {

  override fun execute(progress: ProgressIndicator): VerificationResult {
    val cacheEntry = ideDescriptorsCache.getIdeDescriptorCacheEntry(ideVersion)
    return cacheEntry.use {
      when (cacheEntry) {
        is IdeDescriptorsCache.Result.Found -> {
          val ideDescriptor = cacheEntry.ideDescriptor
          val verificationReportage = createVerificationReportage(progress)
          checkPluginWithIde(ideDescriptor, verificationReportage)
        }
        is IdeDescriptorsCache.Result.NotFound -> throw InterruptedException("IDE $ideVersion is not found")
        is IdeDescriptorsCache.Result.Failed -> throw InterruptedException("Failed to get $ideVersion: ${cacheEntry.message}: ${cacheEntry.error.message}")
      }
    }
  }

  private fun checkPluginWithIde(ideDescriptor: IdeDescriptor,
                                 verificationReportage: VerificationReportage): VerificationResult {
    val dependencyFinder = IdeDependencyFinder(
        ideDescriptor.ide,
        pluginRepository,
        pluginDetailsCache
    )

    val tasks = listOf(PluginVerifier(
        updateInfo,
        verificationReportage,
        emptyList(),
        true,
        pluginDetailsCache,
        DefaultClsResolverProvider(
            dependencyFinder,
            jdkDescriptorsCache,
            jdkPath,
            ideDescriptor,
            PackageFilter(emptyList())
        ),
        VerificationTarget.Ide(ideDescriptor.ideVersion)
    ))
    return verifierExecutor
        .verify(tasks)
        .single()
  }

  private fun createDelegatingReporter(progress: ProgressIndicator): Reporter<Double> {
    return object : Reporter<Double> {
      override fun report(t: Double) {
        progress.fraction = t
      }

      override fun close() = Unit
    }
  }

  private fun createVerificationReportage(progress: ProgressIndicator) = VerificationReportageImpl(
      reporterSetProvider = object : VerificationReportersProvider {
        override val ignoredPluginsReporters: List<Reporter<PluginIgnoredEvent>> = emptyList()

        override val globalMessageReporters = listOf<Reporter<String>>()

        override val globalProgressReporters = listOf(createDelegatingReporter(progress))

        override fun close() = Unit

        override fun getReporterSetForPluginVerification(pluginInfo: PluginInfo, verificationTarget: VerificationTarget) =
            VerificationReporterSet(
                verificationResultReporters = listOf(),
                messageReporters = listOf(),
                progressReporters = listOf(),
                pluginStructureWarningsReporters = emptyList(),
                pluginStructureErrorsReporters = emptyList(),
                problemsReporters = emptyList(),
                dependenciesGraphReporters = listOf(),
                ignoredProblemReporters = emptyList(),
                deprecatedReporters = emptyList(),
                exceptionReporters = listOf()
            )
      }
  )

  /**
   * Verify newer plugins first.
   * This comparison result is used by [org.jetbrains.plugins.verifier.service.tasks.TaskManager]
   * to order tasks execution.
   */
  override fun compareTo(other: VerifyPluginTask) =
      Integer.compare(other.updateInfo.updateId, updateInfo.updateId)

}