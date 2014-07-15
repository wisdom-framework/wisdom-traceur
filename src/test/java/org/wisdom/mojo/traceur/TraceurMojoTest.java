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

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These are just examples to show how watchers can be tested.
 */
public class TraceurMojoTest {

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
        FileUtils.copyFile(new File("src/test/resources/hello.js"), new File(basedir,
                "src/main/resources/assets/doc/hello.js"));
        FileUtils.copyFile(new File("src/test/resources/hello.js"), new File(basedir,
                "src/main/assets/doc/hello.js"));
        mojo.execute();
    }
    @Test
    public void testInitializationWithNoFiles() throws Exception {
        org.wisdom.mojo.traceur.TraceurMojo mojo = new org.wisdom.mojo.traceur.TraceurMojo();
        mojo.basedir = basedir;
        mojo.version = VERSION;
        mojo.buildDirectory = new File(mojo.basedir, "target");
        mojo.execute();
        assertThat(mojo.npm).isNotNull();
    }

    @Test
    public void testInitializationWithUnfilteredInternalAndExternalFilesUsingRegularExtensions() throws
            Exception {
        org.wisdom.mojo.traceur.TraceurMojo mojo = new org.wisdom.mojo.traceur.TraceurMojo();
        mojo.basedir = basedir;
        mojo.version = VERSION;
        mojo.buildDirectory = new File(mojo.basedir, "target");
        FileUtils.copyFile(new File("src/test/resources/hello.js"), new File(basedir,
                "src/main/resources/assets/doc/hello.js"));
        FileUtils.copyFile(new File("src/test/resources/hello.js"), new File(basedir,
                "src/main/assets/doc/hello.js"));
        mojo.execute();

        final File internal = new File(mojo.getInternalAssetOutputDirectory(), "doc/hello.js");
        final File external = new File(mojo.getExternalAssetsOutputDirectory(), "doc/hello.js");
        assertThat(internal).isFile();
        assertThat(external).isFile();

        assertThat(FileUtils.readFileToString(internal)).contains("\"use strict\";")
                .contains("$traceurRuntime.createClass");
        assertThat(FileUtils.readFileToString(external)).contains("\"use strict\";")
                .contains("$traceurRuntime.createClass");
    }

    @Test
    public void testInitializationWithFilteredInternalAndExternalFiles() throws
            Exception {
        org.wisdom.mojo.traceur.TraceurMojo mojo = new org.wisdom.mojo.traceur.TraceurMojo();
        mojo.basedir = basedir;
        mojo.version = VERSION;
        mojo.buildDirectory = new File(mojo.basedir, "target");
        FileUtils.copyFile(new File("src/test/resources/hello.js"), new File(basedir,
                "src/main/resources/assets/doc/hello.js"));
        // Filtered version:
        FileUtils.copyFile(new File("src/test/resources/hello.js"), new File(basedir,
                "target/classes/assets/doc/hello.js"));
        FileUtils.copyFile(new File("src/test/resources/hello.js"), new File(basedir,
                "src/main/assets/doc/hello.js"));
        // Filtered version:
        FileUtils.copyFile(new File("src/test/resources/hello.js"), new File(basedir,
                "target/wisdom/assets/doc/hello.js"));

        mojo.execute();

        final File internal = new File(mojo.getInternalAssetOutputDirectory(), "doc/hello.js");
        final File external = new File(mojo.getExternalAssetsOutputDirectory(), "doc/hello.js");
        assertThat(internal).isFile();
        assertThat(external).isFile();

        assertThat(FileUtils.readFileToString(internal)).contains("\"use strict\";")
                .contains("$traceurRuntime.createClass");
        assertThat(FileUtils.readFileToString(external)).contains("\"use strict\";")
                .contains("$traceurRuntime.createClass");
    }

    @Test
    public void testAccept() throws Exception {
        org.wisdom.mojo.traceur.TraceurMojo mojo = new org.wisdom.mojo.traceur.TraceurMojo();
        mojo.basedir = basedir;
        mojo.version = VERSION;
        mojo.buildDirectory = new File(mojo.basedir, "target");
        mojo.execute();

        assertThat(mojo.accept(new File("hello.js"))).isTrue();
        assertThat(mojo.accept(new File("hello.markdown"))).isFalse();
        assertThat(mojo.accept(new File("hello.asciidoc"))).isFalse();
        assertThat(mojo.accept(new File("hello.html"))).isFalse();
    }
}
