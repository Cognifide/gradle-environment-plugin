package com.cognifide.gradle.environment.docker

import org.buildobjects.process.ExternalProcessFailureException
import org.buildobjects.process.ProcBuilder
import org.buildobjects.process.ProcResult
import org.buildobjects.process.TimeoutException
import org.gradle.process.internal.streams.SafeStreams
import org.apache.commons.lang3.StringUtils

@Suppress("TooGenericExceptionCaught")
object DockerProcess {

    const val COMMAND = "docker"

    fun exec(options: ProcBuilder.() -> Unit): ProcResult {
        return try {
            ProcBuilder(COMMAND)
                    .withNoTimeout()
                    .withOutputStream(SafeStreams.systemOut())
                    .withErrorStream(SafeStreams.systemErr())
                    .apply(options)
                    .run()
        } catch (e: Exception) {
            throw composeException(e)
        }
    }

    fun execQuietly(options: ProcBuilder.() -> Unit): ProcResult {
        return try {
            ProcBuilder(COMMAND)
                    .ignoreExitStatus()
                    .apply(options)
                    .run()
        } catch (e: Exception) {
            throw composeException(e)
        }
    }

    fun execString(options: ProcBuilder.() -> Unit): String {
        return try {
            ProcBuilder(COMMAND)
                    .withNoTimeout()
                    .apply(options)
                    .run()
                    .outputString.trim()
        } catch (e: Exception) {
            throw composeException(e)
        }
    }

    @Suppress("SpreadOperator")
    fun execSpec(spec: DockerSpec): DockerResult {
        return DockerResult(exec {
            withArgs(*spec.args.toTypedArray())
            withExpectedExitStatuses(spec.exitCodes.toSet())

            spec.input?.let { withInputStream(it) }
            spec.output?.let { withOutputStream(it) }
            spec.errors?.let { withErrorStream(it) }
        })
    }

    private fun composeException(e: Exception): DockerException {
        return when (e) {
            is ExternalProcessFailureException -> DockerException("Docker command process failure!" +
                    " Command: '${e.command}', error: '${e.stderr}', exit code: '${e.exitValue}'", e)
            is TimeoutException -> DockerException("Docker command timeout! Error: '${e.message}'", e)
            else -> DockerException("Docker command unknown failure. Error: '${e.message}'", e)
        }
    }

    /**
     * Splits command to arguments usually delimited by space
     * while considering quoted string containing spaces as single argument.
     */
    fun commandToArgs(command: String): List<String> {
        val quotedSpaceToken = "@@@SPACE@@@"
        var tokenizedCommand = command

        Regex("'([^']+)'").findAll(command).iterator().forEachRemaining {
            val quotedString = it.groupValues[1]
            val tokenizedString = quotedString.replace(" ", quotedSpaceToken)
            tokenizedCommand = tokenizedCommand.replace("'$quotedString'", tokenizedString)
        }

        return StringUtils.split(tokenizedCommand, " ").map { it.replace(quotedSpaceToken, " ") }
    }
}
