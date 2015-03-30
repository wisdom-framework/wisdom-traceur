/*
 * #%L
 * Wisdom-Framework
 * %%
 * Copyright (C) 2013 - 2014 Wisdom Framework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wisdom.mojo.traceur;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.wisdom.maven.WatchingException;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class TraceurTest {
    private static final String VERSION = "0.0.87";
    File basedir = new File("target/workbench/project");

    @Before
    public void setUp() {
        FileUtils.deleteQuietly(basedir);
    }


    @Test
    public void testThatES5remainsES5() throws
            Exception {
        org.wisdom.mojo.traceur.TraceurMojo mojo = new org.wisdom.mojo.traceur.TraceurMojo();
        mojo.basedir = basedir;
        mojo.version = VERSION;
        mojo.buildDirectory = new File(mojo.basedir, "target");
        mojo.output = "acme.js";
        mojo.moduleStrategy = "inline";
        mojo.execute();
        FileUtils.copyFile(new File("src/test/resources/dummy.es6.js"), new File(basedir,
                "src/main/resources/assets/doc/dummy.js"));
        FileUtils.copyFile(new File("src/test/resources/dummy.es6.js"), new File(basedir,
                "src/main/assets/doc/dummy.js"));
        mojo.execute();

        final File internal = new File(mojo.getInternalAssetOutputDirectory(), "acme.js");
        final File external = new File(mojo.getExternalAssetsOutputDirectory(), "acme.js");
        assertThat(internal).doesNotExist();
        assertThat(external).doesNotExist();
    }

    @Test
    public void testCompilationError() throws
            Exception {
        org.wisdom.mojo.traceur.TraceurMojo mojo = new org.wisdom.mojo.traceur.TraceurMojo();
        mojo.basedir = basedir;
        mojo.version = VERSION;
        mojo.buildDirectory = new File(mojo.basedir, "target");
        mojo.output = "acme.js";
        mojo.moduleStrategy = "inline";
        mojo.execute();

        File source = new File(basedir,
                "src/main/resources/assets/doc/erroneous.es6.js");
        FileUtils.copyFile(new File("src/test/resources/erroneous.es6.js"), source);

        try {
            mojo.fileCreated(source);
            fail("Compilation error expected");
        } catch (WatchingException e) {
            // Excepted exception
            assertThat(e.getFile().getName()).isEqualTo("erroneous.es6.js");
            assertThat(e.getLine()).isEqualTo(11);
            assertThat(e.getCharacter()).isGreaterThan(0);
            assertThat(e.getTitle()).contains("Compilation");
            assertThat(e.getMessage()).isEqualTo("Unexpected end of input");
        }
    }

    @Test
    public void testFileIncludes() throws Exception {
        org.wisdom.mojo.traceur.TraceurMojo mojo = new org.wisdom.mojo.traceur.TraceurMojo();
        mojo.basedir = basedir;
        mojo.version = VERSION;
        mojo.buildDirectory = new File(mojo.basedir, "target");
        mojo.output = "acme.js";
        mojo.moduleStrategy = "inline";
        mojo.includes = new String[]{"human*.es6.js"};

        // Copy the files that do not have the bang comment

        FileUtils.copyFile(new File("src/test/resources/earth/human.es6.js"), new File(basedir,
                "src/main/resources/assets/earth/human.es6.js"));
        FileUtils.copyFile(new File("src/test/resources/earth/humans.es6.js"), new File(basedir,
                "src/main/resources/assets/earth/humans.es6.js"));

        mojo.execute();

        final File output = new File(mojo.getInternalAssetOutputDirectory(), "acme.js");
        assertThat(output).isFile();
    }

    @Test
    public void testFileIncludesWithANonIncludedFile() throws Exception {
        org.wisdom.mojo.traceur.TraceurMojo mojo = new org.wisdom.mojo.traceur.TraceurMojo();
        mojo.basedir = basedir;
        mojo.version = VERSION;
        mojo.buildDirectory = new File(mojo.basedir, "target");
        mojo.output = "acme.js";
        mojo.moduleStrategy = "inline";
        mojo.includes = new String[]{"human*.es6.js"};

        // Copy the files that do not have the bang comment

        FileUtils.copyFile(new File("src/test/resources/earth/human.es6.js"), new File(basedir,
                "src/main/resources/assets/earth/human.es6.js"));
        FileUtils.copyFile(new File("src/test/resources/earth/humans.es6.js"), new File(basedir,
                "src/main/resources/assets/earth/humans.es6.js"));
        FileUtils.copyFile(new File("src/test/resources/earth/dummy.es6.js"), new File(basedir,
                "src/main/resources/assets/earth/dummy.es6.js"));

        mojo.execute();

        final File output = new File(mojo.getInternalAssetOutputDirectory(), "acme.js");
        assertThat(output).isFile();
        assertThat(FileUtils.readFileToString(output)).doesNotContain("Bob");
    }
}