package com.cognifide.gradle.environment.docker

import com.cognifide.gradle.environment.EnvironmentExtension
import org.gradle.process.internal.streams.SafeStreams
import java.io.File

open class RunSpec(environment: EnvironmentExtension) : DockerDefaultSpec(environment) {

    var name: String? = null

    var image: String = ""

    var volumes = mapOf<String, String>()

    var ports = mapOf<String, String>()

    fun port(hostPort: Int, containerPort: Int) = port(hostPort.toString(), containerPort.toString())

    fun port(hostPort: String, containerPort: String) {
        ports = ports + (hostPort to containerPort)
    }

    fun port(port: Int) = port(port, port)

    fun volume(localFile: File, containerPath: String) = volume(localFile.absolutePath, containerPath)

    fun volume(localPath: String, containerPath: String) {
        volumes = volumes + (localPath to containerPath)
    }

    var cleanup: Boolean = false

    var detached: Boolean = false

    private var operationProvider: () -> String = {
        when {
            fullCommand.isBlank() -> "Running image '$image'"
            else -> "Running image '$image' and command '$fullCommand'"
        }
    }

    val operation: String get() = operationProvider()

    fun operation(textProvider: () -> String) {
        this.operationProvider = textProvider
    }

    fun operation(text: String) = operation { text }

    var indicator = true

    init {
        input = SafeStreams.emptyInput()
        output = SafeStreams.systemOut()
        errors = SafeStreams.systemErr()
    }
}
