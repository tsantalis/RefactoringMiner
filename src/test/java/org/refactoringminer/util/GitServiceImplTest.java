package org.refactoringminer.util;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GitServiceImplTest {

    @TempDir
    private static Path tmp;

    @Test
    @Order(1)
    void testCloneIfNotExists_EmptyDirExists() throws Exception {
        GitServiceImpl gitService = new GitServiceImpl();
        File tmpFile = tmp.toFile();
        Assumptions.assumeTrue(tmpFile.exists() && tmpFile.list().length == 0);
        try (Repository repo = gitService.cloneIfNotExists(tmp.toString(),"https://github.com/bacen/pix-api.git")) {
            assertTrue(repo.getDirectory().exists() && repo.getIndexFile().exists());
        }
    }
    @Test
    @Order(2)
    void testCloneIfNotExists_ClonedRepoExists() throws Exception {
        GitServiceImpl gitService = new GitServiceImpl();
        File tmpFile = tmp.toFile();
        Assumptions.assumeTrue(tmpFile.exists() && tmpFile.list().length > 0);
        try (Repository repo = gitService.cloneIfNotExists(tmp.toString(),"https://github.com/bacen/pix-api.git")) {
            assertTrue(repo.getDirectory().exists() && repo.getIndexFile().exists());
        }
    }
    @Test
    @Order(3)
    void testCloneIfNotExists_DirDoesNotExists() throws Exception {
        FileUtils.deleteDirectory(tmp.toFile());
        GitServiceImpl gitService = new GitServiceImpl();
        try (Repository repo = gitService.cloneIfNotExists(tmp.toString(),"https://github.com/bacen/pix-api.git")) {
            assertTrue(repo.getDirectory().exists() && repo.getIndexFile().exists());
        }
    }

}