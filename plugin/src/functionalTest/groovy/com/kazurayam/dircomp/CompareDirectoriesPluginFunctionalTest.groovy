package com.kazurayam.dircomp

import spock.lang.Specification
import spock.lang.TempDir
import org.gradle.testkit.runner.GradleRunner

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CompareDirectoriesPluginFunctionalTest extends Specification {

    // fields
    @TempDir
    private File tempDir

    private static Path projectDir
    private static Path fixturesDir
    private Path outputFile
    private Path diffDir

    // fixture methods
    def setupSpec() {
        projectDir = Paths.get(".").toAbsolutePath().normalize()
        fixturesDir = projectDir.resolve("src/test/fixtures").toAbsolutePath()
    }

    def setup() {
        outputFile = tempDir.toPath().resolve( "build/tmp/differences.json").toAbsolutePath()
        diffDir = tempDir.toPath().resolve("build/tmp/diff").toAbsolutePath()
        settingsFile << ""
        buildFile << """
plugins {
    id('com.kazurayam.compare-directories')
}

compareDirectories {
    dirA = file("${fixturesDir.toString()}/A")
    dirB = file("${fixturesDir.toString()}/B")
    outputFile = file("build/tmp/differences.json")
    diffDir = file("build/tmp/diff")
}

task dircomp(type: com.kazurayam.dircomp.CompareDirectoriesTask) {
    dirA = file("${fixturesDir.toString()}/A")
    dirB = file("${fixturesDir.toString()}/B")
    outputFile = file("build/tmp/differences.json")
    diffDir = file("build/tmp/diff")
    doFirst {
        println "dircomp.doFirst was executed"
    }
    doLast {
        println "dircomp2.doLast was executed"
    }
}
"""
    }
    //def cleanup() {}
    //def cleanupSpec() {}

    // feature methods
    def "can run compareDirectories task"() {
        given:
        assert Files.exists(fixturesDir)
        println "fixturesDir=${fixturesDir.toString()}"
        Files.createDirectories(tempDir.toPath().resolve("build"))

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("compareDirectories")
        runner.withProjectDir(tempDir)
        def result = runner.build()
        String message = outputFile.toFile().text
        println "[CompareDirectoriesPluginFunctionalTest]"
        println message

        then:
        message.contains("filesOnlyInA")
        message.contains("filesOnlyInB")
        message.contains("intersection")
        message.contains("modifiedFiles")
    }

    def "can run dircomp task"() {
        given:
        assert Files.exists(fixturesDir)
        println "fixturesDir=${fixturesDir.toString()}"
        Files.createDirectories(tempDir.toPath().resolve("build"))

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("dircomp") // THIS IS THE DIFFERENCE
        runner.withProjectDir(tempDir)
        def result = runner.build()
        String message = outputFile.toFile().text
        println "[CompareDirectoriesPluginFunctionalTest]"
        println message

        then:
        message.contains("filesOnlyInA")
        message.contains("filesOnlyInB")
        message.contains("intersection")
        message.contains("modifiedFiles")
    }

    // helper methods
    private Path getBuildFile() {
        return tempDir.toPath().resolve("build.gradle")
    }

    private Path getSettingsFile() {
        return tempDir.toPath().resolve("settings.gradle")
    }
}
