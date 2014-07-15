package org.wisdom.mojo.traceur;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.wisdom.maven.WatchingException;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class TraceurTest {
    private static final String VERSION = "0.0.49";
    File basedir = new File("target/workbench/project");

    @Before
    public void setUp() {
        FileUtils.deleteQuietly(basedir);
    }

    @Test
    public void testCompilation() throws Exception {
        org.wisdom.mojo.traceur.TraceurMojo mojo = new org.wisdom.mojo.traceur.TraceurMojo();
        mojo.basedir = basedir;
        mojo.version = VERSION;
        mojo.buildDirectory = new File(mojo.basedir, "target");
        FileUtils.copyFile(new File("src/test/resources/dummy.js"), new File(basedir,
                "src/main/resources/assets/doc/dummy.js"));
        FileUtils.copyFile(new File("src/test/resources/dummy.js"), new File(basedir,
                "src/main/assets/doc/dummy.js"));
        mojo.execute();
    }

    @Test
    public void testThatES5remainsES5() throws
            Exception {
        org.wisdom.mojo.traceur.TraceurMojo mojo = new org.wisdom.mojo.traceur.TraceurMojo();
        mojo.basedir = basedir;
        mojo.version = VERSION;
        mojo.buildDirectory = new File(mojo.basedir, "target");
        FileUtils.copyFile(new File("src/test/resources/dummy.js"), new File(basedir,
                "src/main/resources/assets/doc/dummy.js"));
        FileUtils.copyFile(new File("src/test/resources/dummy.js"), new File(basedir,
                "src/main/assets/doc/dummy.js"));
        mojo.execute();

        final File internal = new File(mojo.getInternalAssetOutputDirectory(), "doc/dummy.js");
        final File external = new File(mojo.getExternalAssetsOutputDirectory(), "doc/dummy.js");
        assertThat(internal).isFile();
        assertThat(external).isFile();

        assertThat(FileUtils.readFileToString(internal)).doesNotContain("\"use strict\";")
                .doesNotContain("$traceurRuntime");
        assertThat(FileUtils.readFileToString(external)).doesNotContain("\"use strict\";")
                .doesNotContain("$traceurRuntime");
    }
    @Test
    public void testCompilationError() throws
            Exception {
        org.wisdom.mojo.traceur.TraceurMojo mojo = new org.wisdom.mojo.traceur.TraceurMojo();
        mojo.basedir = basedir;
        mojo.version = VERSION;
        mojo.buildDirectory = new File(mojo.basedir, "target");
        mojo.execute();

        File source = new File(basedir,
                "src/main/resources/assets/doc/erroneous.js");
        FileUtils.copyFile(new File("src/test/resources/erroneous.js"), source);

        try {
            mojo.fileCreated(source);
            fail("Compilation error expected");
        } catch (WatchingException e) {
            // Excepted exception
            assertThat(e.getFile().getName()).isEqualTo("erroneous.js");
            assertThat(e.getLine()).isEqualTo(10);
            assertThat(e.getCharacter()).isGreaterThan(0);
            assertThat(e.getTitle()).contains("Compilation");
            assertThat(e.getMessage()).isEqualTo("Unexpected end of input");
        }
    }
}