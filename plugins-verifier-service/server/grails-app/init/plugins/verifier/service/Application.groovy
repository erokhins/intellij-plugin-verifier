package plugins.verifier.service

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.DiskSpaceHealthIndicatorProperties

class Application extends GrailsAutoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(Application.class)

  static void main(String[] args) {
    setSystemProperties()

    LOG.info("Server settings: ${Settings.values().collect { it.key + "=" + it.get() }.join(", ")}")

    def context = GrailsApp.run(Application, args)

  }

  private static void setSystemProperties() {
    String appHomeDir = Settings.APP_HOME_DIRECTORY.get()
    System.setProperty("plugin.verifier.home.dir", appHomeDir + "/verifier")
    System.setProperty("intellij.structure.temp.dir", appHomeDir + "/intellijStructureTmp")
  }

  @Override
  Closure doWithSpring() {
    { ->
      diskSpaceHealthIndicatorProperties(DiskSpaceHealthIndicatorProperties) {
        // Set threshold to 250MB.
        threshold = 250 * 1024 * 1024
      }
    }
  }
}