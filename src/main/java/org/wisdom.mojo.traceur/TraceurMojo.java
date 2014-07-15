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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.wisdom.maven.Constants;
import org.wisdom.maven.WatchingException;
import org.wisdom.maven.mojos.AbstractWisdomWatcherMojo;
import org.wisdom.maven.node.NPM;
import org.wisdom.maven.utils.WatcherUtils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Traceur((https://github.com/google/traceur-compiler) is a JavaScript
 * .next-to-JavaScript-of-today compiler that allows you to use features from the future today.
 * Traceur's goal is to inform the design of new JavaScript features which are only valuable if
 * they allow you to write better code . Traceur allows you to try out new and proposed language
 * features today, helping you say what you mean in your code while informing the standards process.
 * <p/>
 * The Wisdom Traceur extension generates valid EcmaScript 5 (in other words, regular JavaScript) from
 * EcmaScript 6 by relying on Traceur. It supports the _watch_ mode, so every modification triggers
 * the file to be recompiled.
 */
@Mojo(name = "compile-es6", threadSafe = false,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresProject = true,
        defaultPhase = LifecyclePhase.COMPILE)
public class TraceurMojo extends AbstractWisdomWatcherMojo implements Constants {

    /**
     * The extension of output files.
     */
    public static final String OUTPUT_EXTENSION = "js";

    /**
     * The extension of the input files.
     */
    public static final String INPUT_EXTENSION = "js";

    /**
     * The name of the NPM.
     */
    public static final String NPM_NAME = "traceur";

    /**
     * The most current release version of Traceur.
     */
    @Parameter(defaultValue = "0.0.49")
    protected String version;

    /**
     * The NPM object.
     */
    protected NPM npm;

    /**
     * Compiles all EcmaScripts(JavaScripts) files located in the internal and external asset
     * directories.
     * <p/>
     * This is the main Mojo entry point. The {@code execute} method is invoked by the regular Maven execution.
     *
     * @throws MojoExecutionException if a JavaScript file cannot be processed.
     */
    public void execute()
            throws MojoExecutionException {

        npm = NPM.npm(this, NPM_NAME, version);

        try {
            // The getResources method locates all the assets files from "src/main/resources/assets" (internal
            // assets) and "src/main/assets" (external assets) having on of the given extensions.
            for (File f : getResources(ImmutableList.of("js"))) {
                process(f);
            }
        } catch (WatchingException e) {
            throw new MojoExecutionException("Error while processing a JavaScript file", e);
        }

    }

    /**
     * Checks if a filtered version of the input file exists, and uses this one, otherwise
     * it will use the unfiltered version. Then Compiles the input JavaScript 6 file into valid
     * JavaScript 5 using traceur executed by NPM. The traceur command takes as agruments --out,
     * output file destination, --script, input file's location.
     *
     * @param input the Javascript file to compile.
     * @throws WatchingException if a compilation error occurs.
     */
    public void process(File input) throws WatchingException {
        // The file may have been filtered (copied to the output directory and placeholders have been filled with
        // actual values. In this case, use the filtered version.
        File filtered = getFilteredVersion(input);
        if (filtered == null) {
            // It was not copied.
            getLog().warn("Cannot find the filtered version of " + input.getAbsolutePath() + ", " +
                    "using source file.");
            filtered = input;
        }

        // Call traceur.
        File output = getOutputFile(input);
        try {
            npm.execute("traceur", "--out", output.getAbsolutePath(), "--script",
                    filtered.getAbsolutePath());
        } catch (MojoExecutionException e) {
            if (!Strings.isNullOrEmpty(npm.getLastErrorStream())) {
                throw build(npm.getLastErrorStream(), input);
            } else {
                throw new WatchingException("EcmaScript 6 Compilation Error",
                        "Error while compiling " + input.getAbsolutePath(), input, e);
            }
        }
    }

    /**
     * Regexp pattern to display Traceur compilation errors in a clean manner.
     */
    public static Pattern TRACEUR_COMPILATION_ERROR = Pattern.compile("\\[Error: (.*):([0-9]*):([0-9]*):(.*)");

    /**
     * Builds the WatchingException by searching the error msg, invoked during the compilation of
     * the EcmaScript(JavaScript) 6, for specified patterns. If no
     * matching expressions are found then the original error msg is returned.
     *
     * @param message The message from last error stream.
     * @param source  is the current file either being created, processed, or updated.
     * @return a WatchingException for EcmaScript compilation errors.
     */
    public WatchingException build(String message, File source) {
        String[] lines = message.split("\n");
        for (String l : lines) {
            if (!Strings.isNullOrEmpty(l)) {
                message = l.trim();
                break;
            }
        }
        final Matcher matcher = TRACEUR_COMPILATION_ERROR.matcher(message);
        if (matcher.matches()) {
            String line = matcher.group(2);
            String character = matcher.group(3);
            String reason = matcher.group(4).trim();
            return new WatchingException("EcmaScript 6 Compilation Error", reason, source,
                    Integer.valueOf(line), Integer.valueOf(character), null);
        } else {
            return new WatchingException("EcmaScript 6 Compilation Error", message, source, null);
        }
    }

    /**
     * Checks to make sure the input file is the acceptable type .js
     *
     * @param file is the file.
     * @return true if the file is js, otherwise false.
     */
    @Override
    public boolean accept(File file) {
        return WatcherUtils.hasExtension(file, ImmutableList.of(INPUT_EXTENSION));
    }

    /**
     * Creates a new file by calling the {@link #process(java.io.File)} method.
     *
     * @param file is the file.
     * @return true always.
     * @throws WatchingException that is created if there is compilation errors during process.
     */
    @Override
    public boolean fileCreated(File file) throws WatchingException {
        process(file);
        return true;
    }

    /**
     * Updates the javascript input file by calling {@link #fileCreated(java.io.File)} method.
     *
     * @param file is the file.
     * @return true always.
     * @throws WatchingException that is created if there is compilation errors during process.
     */
    @Override
    public boolean fileUpdated(File file) throws WatchingException {
        return fileCreated(file);
    }

    /**
     * Deletes the output file of the given file.
     *
     * @param file the file
     * @return true always.
     */
    @Override
    public boolean fileDeleted(File file) {
        File output = getOutputFile(file, OUTPUT_EXTENSION);
        FileUtils.deleteQuietly(output);
        return true;
    }
}
