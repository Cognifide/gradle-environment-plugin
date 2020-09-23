package com.cognifide.gradle.environment.hosts

import com.cognifide.gradle.environment.EnvironmentExtension
import org.gradle.internal.os.OperatingSystem

class HostUpdater(val environment: EnvironmentExtension) {

    private val project = environment.project

    private val logger = project.logger

    private val common = environment.common

    val workDir = common.obj.dir {
        convention(environment.rootDir.dir("hosts"))
        common.prop.file("environment.hosts.updater.workDir")?.let { set(it) }
    }

    val targetFile = common.obj.file {
        fileProvider(common.obj.provider {
            project.file(when {
                OperatingSystem.current().isWindows -> """C:\Windows\System32\drivers\etc\hosts"""
                else -> "/etc/hosts"
            })
        })
        common.prop.file("environment.hosts.updater.targetFile")?.let { set(it) }
    }

    val section = common.obj.string {
        convention(environment.docker.stack.internalName)
        common.prop.string("environment.hosts.updater.section")?.let { set(it) }
    }

    @Suppress("MaxLineLength")
    fun update() {
        val os = OperatingSystem.current()
        val osFile = targetFile.get()

        val dir = workDir.get().asFile.apply { mkdirs() }

        val entriesFile = dir.resolve("hosts.txt").apply {
            logger.info("Generating hosts entries file: $this")
            writeText(environment.hosts.defined.get().joinToString(System.lineSeparator()) { it.text })
        }
        val updaterJar = dir.resolve("hosts.jar").apply {
            logger.info("Providing hosts updater program: $this")
            outputStream().use { output ->
                this@HostUpdater.javaClass.getResourceAsStream("/hosts.jar").use {
                    input -> input.copyTo(output)
                }
            }
        }
        val sectionName = section.get()

        if (os.isWindows) {
            val scriptFile = dir.resolve("hosts.bat")
            logger.info("Generating hosts updating script: $scriptFile")

            scriptFile.writeText("""
                powershell -command "Start-Process cmd -ArgumentList '/C cd %CD% && java -jar $updaterJar $sectionName $entriesFile $osFile' -Verb runas"
            """.trimIndent())
            project.exec { it.commandLine("cmd", "/C", scriptFile.toString()) }
            logger.lifecycle("Environment hosts successfully updated.")
        } else {
            val scriptFile = dir.resolve("hosts.sh")
            logger.info("Generating hosts updating script: $scriptFile")

            if (os.isMacOsX) {
                scriptFile.writeText("""
                    #!/bin/sh
                    osascript -e "do shell script \"java -jar $updaterJar $sectionName $entriesFile $osFile\" with prompt \"Gradle Environment Hosts\" with administrator privileges" 
                """.trimIndent())
                project.exec { it.commandLine("sh", scriptFile.toString()) }
                logger.lifecycle("Environment hosts successfully updated.")
            } else {
                scriptFile.writeText("""
                    #!/bin/sh
                    java -jar $updaterJar $sectionName $entriesFile $osFile
                """.trimIndent())
                logger.lifecycle("To update environment hosts, run script below as administrator/super-user:\n$scriptFile")
            }
        }
    }
}
