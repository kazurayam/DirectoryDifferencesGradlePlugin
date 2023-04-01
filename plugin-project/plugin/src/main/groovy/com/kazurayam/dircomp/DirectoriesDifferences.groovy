package com.kazurayam.dircomp

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.AbstractDelta
import com.github.difflib.patch.Patch

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

class DirectoriesDifferences {

    private Path baseDir

    private Path dirA

    private Path dirB

    /**
     * The files found only in the first directory (A)
     */
    private Set<String> filesOnlyInA

    /**
     * The files found only in the second directory (B)
     */
    private Set<String> filesOnlyInB

    private Set<String> intersection

    /**
     * The files existing in both directories but have different content
     */
    private Set<String> modifiedFiles

    /*
     * com.fasterxml.jackson.databind requires the default constructor without args
     */
    DirectoriesDifferences() {
        this.baseDir = null;
        this.dirA = null;
        this.dirB = null;
        this.filesOnlyInA = new HashSet<>();
        this.filesOnlyInB = new HashSet<>();
        this.intersection = new HashSet<>();
        this.modifiedFiles = new HashSet<>();
    }

    DirectoriesDifferences(Path baseDir,
                           Path dirA,
                           Path dirB,
                           Set<String> filesOnlyInA,
                           Set<String> filesOnlyInB,
                           Set<String> intersection,
                           Set<String> modifiedFiles) {
        this.baseDir = baseDir.normalize().toAbsolutePath()
        this.dirA = dirA.normalize()
        this.dirB = dirB.normalize()
        this.filesOnlyInA = filesOnlyInA
        this.filesOnlyInB = filesOnlyInB
        this.intersection = intersection
        this.modifiedFiles = modifiedFiles
    }

    Path getDirA() {
        return dirA
    }

    Path getDirB() {
        return dirB
    }

    void setFilesOnlyInA(Collection<String> filesOnlyInA) {
        this.filesOnlyInA = new HashSet<>(filesOnlyInA);
    }

    List<String> getFilesOnlyInA() {
        return filesOnlyInA.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    public void setFilesOnlyInB(Collection<String> filesOnlyInB) {
        this.filesOnlyInB = new HashSet<>(filesOnlyInB);
    }

    List<String> getFilesOnlyInB() {
        return filesOnlyInB.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    void setIntersection(Collection<String> intersection) {
        this.intersection = new HashSet<>(intersection);
    }

    List<String> getIntersection() {
        return intersection.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    public void setModifiedFiles(Collection<String> modifiedFiles) {
        this.modifiedFiles = new HashSet<>(modifiedFiles);
    }

    List<String> getModifiedFiles() {
        return modifiedFiles.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    String toString() {
        return serialize();
    }

    String serialize() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonResult =
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
            return jsonResult;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    static DirectoriesDifferences deserialize(File json) {
        return deserialize(json.toPath());
    }

    static DirectoriesDifferences deserialize(Path jsonFile) {
        try {
            String json = String.join("\n", Files.readAllLines(jsonFile));
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, DirectoriesDifferences.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return number of the diff files created
     */
    int makeDiffFiles(Path diffDir) {
        Objects.requireNonNull(diffDir)
        assert Files.exists(diffDir)
        int result = 0
        this.getModifiedFiles().forEach {relativePath ->
            //println "relativePath: " + relativePath
            try {
                Path fileA = this.getDirA().resolve(relativePath).toAbsolutePath()
                Path fileB = this.getDirB().resolve(relativePath).toAbsolutePath()
                List<String> textA = Files.readAllLines(fileA)
                List<String> textB = Files.readAllLines(fileB)
                // generating diff information
                Patch<String> diff = DiffUtils.diff(textA, textB)

                // generating unified diff format
                String relativePathA = dirA.toString() + "/" + relativePath
                String relativePathB = dirB.toString() + "/" + relativePath
                List<String> unifiedDiff =
                        UnifiedDiffUtils.generateUnifiedDiff(
                                relativePathA, relativePathB, textA, diff, 0)
                // debug
                println "unifiedDiff.size()=" + unifiedDiff.size()
                unifiedDiff.each {
                    println it
                }
                //
                String dirAName = this.getDirA().getFileName().toString()
                String dirBName = this.getDirB().getFileName().toString()
                Path diffOutputFile =
                        diffDir.resolve(dirAName + "_" + dirBName)
                                .resolve(relativePath)
                Files.createDirectories(diffOutputFile.getParent())
                BufferedWriter br =
                        new BufferedWriter(
                                new OutputStreamWriter(
                                        new FileOutputStream(diffOutputFile.toFile()),
                                        "UTF-8"))
                // print the unified diff into file
                for (String line : unifiedDiff) {
                    br.println(line)
                }
                br.flush()
                br.close()

                println "diffOutputFile=" + diffOutputFile.toString()

                result += 1
            } catch (Exception e) {
                e.printStackTrace()
                throw e
            }
        }
        return result
    }
}