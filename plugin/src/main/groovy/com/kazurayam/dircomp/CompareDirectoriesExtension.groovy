package com.kazurayam.dircomp

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class CompareDirectoriesExtension {

    abstract Property<ConfigurableFileTree> getDirA()
    abstract Property<ConfigurableFileTree> getDirB()
    abstract RegularFileProperty getOutputFile()
    abstract DirectoryProperty getDiffDir()

}
