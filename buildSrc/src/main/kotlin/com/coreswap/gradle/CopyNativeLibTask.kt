package com.coreswap.gradle

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class CopyNativeLibTask @Inject constructor(
    private val fsOps: FileSystemOperations,
) : DefaultTask() {
    @get:Input
    abstract val androidAbi: Property<String>

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copy() {
        fsOps.copy {
            from(inputFile.get())
            into(outputDirectory.get().asFile.resolve(androidAbi.get()))
        }
    }
}
