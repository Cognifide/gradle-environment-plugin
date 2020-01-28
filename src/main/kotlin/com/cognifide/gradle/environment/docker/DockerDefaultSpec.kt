package com.cognifide.gradle.environment.docker

import com.cognifide.gradle.environment.EnvironmentExtension
import java.io.InputStream
import java.io.OutputStream

open class DockerDefaultSpec(protected val environment: EnvironmentExtension) : DockerSpec {

    override var command: String = ""

    override val args: List<String>
        get() = mutableListOf<String>().apply {
            addAll(options)
            addAll(DockerProcess.commandToArgs(command))
        }

    override val fullCommand: String
        get() = args.joinToString(" ")

    override var options: List<String> = listOf()

    override fun option(value: String) {
        options = options + value
    }

    override var exitCodes: List<Int> = listOf(0)

    fun exitCode(code: Int) {
        exitCodes = listOf(code)
    }

    override fun ignoreExitCodes() {
        exitCodes = listOf()
    }

    override var input: InputStream? = null

    override var output: OutputStream? = null

    override var errors: OutputStream? = null
}
