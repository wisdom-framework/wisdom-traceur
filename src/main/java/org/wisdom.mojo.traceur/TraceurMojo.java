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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Traceur((https://github.com/google/traceur-compiler) is a JavaScript
 * .next-to-JavaScript-of-today compiler that allows you to use features from the future today.
 * Traceur's goal is to inform the design of new JavaScript features which are only valuable if
 * they allow you to write better code . Traceur allows you to try out new and proposed language
 * features today, helping you say what you mean in your code while informing the standards process.
 * <p>
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
    public static final String INPUT_EXTENSION = ".es6.js";

    /**
     * The name of the NPM.
     */
    public static final String NPM_NAME = "traceur";

    /**
     * Regexp pattern to display Traceur compilation errors in a clean manner.
     */
    public static final Pattern TRACEUR_COMPILATION_ERROR = Pattern.compile("\\[Error: (.*):" +
            "([0-9]*):([0-9]*):(.*)");
    public static final String ERROR_TITLE = "EcmaScript 6 Compilation Error";

    /**
     * The most current release version of Traceur.
     */
    @Parameter(defaultValue = "0.0.49")
    protected String version;

    @Parameter(defaultValue = "true")
    protected boolean experimental;

    @Parameter(defaultValue = "inline")
    protected String moduleStrategy;

    @Parameter(defaultValue = "${project.artifactId}.js")
    protected String output;

    /**
     * The NPM object.
     */
    protected NPM npm;

    /**
     * Compiles all EcmaScripts(JavaScripts) files located in the internal and external asset
     * directories.
     * <p>
     * This is the main Mojo entry point. The {@code execute} method is invoked by the regular Maven execution.
     *
     * @throws MojoExecutionException if a JavaScript file cannot be processed.
     */
    public void execute()
            throws MojoExecutionException {

        npm = NPM.npm(this, NPM_NAME, version);

        compile();
    }

    public void compile() throws MojoExecutionException {
        Collection<File> internals = WatcherUtils.getAllFilesFromDirectory
                (getInternalAssetsDirectory(), ImmutableList.of("es6.js"));

        Collection<File> externals = WatcherUtils.getAllFilesFromDirectory
                (getExternalAssetsDirectory(), ImmutableList.of("es6.js"));

        try {
            compile(getInternalAssetOutputDirectory(), internals);
            compile(getExternalAssetsOutputDirectory(), externals);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void compile(File dir, Collection<File> files) throws MojoExecutionException, IOException {
        if (!files.isEmpty()) {
            File output = new File(dir, this.output);
            getLog().info("Compiling EcmaScript files : " + files + " to " + output
                    .getAbsolutePath());
            List<String> args = new ArrayList<String>();
            args.add("--out");
            args.add(output.getAbsolutePath());
            for (File file : files) {
                // The input files have the .es6.js extension. We copy them and use the .js extension.
                // Indeed, .es6.js extension make module import use name.es6 instead of just name.
                File fileToCompile = null;
                File filtered = getFilteredVersion(file);
                if (filtered != null) {
                    fileToCompile = new File(filtered.getParentFile(), file.getName().replace(".es6", ""));
                    FileUtils.copyFile(filtered, fileToCompile);
                } else {
                    File out = getOutputFile(file);
                    fileToCompile = new File(out.getParentFile(), file.getName().replace(".es6", ""));
                    FileUtils.copyFile(file, fileToCompile);
                }
                args.add(fileToCompile.getAbsolutePath());
            }

            if (experimental) {
                args.add("--experimental");
            }

            args.add("--modules=" + moduleStrategy);
            npm.execute("traceur", args.toArray(new String[args.size()]));
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
        try {
            compile();
        } catch (MojoExecutionException e) {
            if (!Strings.isNullOrEmpty(npm.getLastErrorStream())) {
                throw build(npm.getLastErrorStream(), input);
            } else {
                throw new WatchingException(ERROR_TITLE,
                        "Error while compiling " + input.getAbsolutePath(), input, e);
            }
        }
    }

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
            return new WatchingException(ERROR_TITLE, reason, source,
                    Integer.valueOf(line), Integer.valueOf(character), null);
        } else {
            return new WatchingException(ERROR_TITLE, message, source, null);
        }
    }

    /**
     * Checks to make sure the input file is the acceptable type .js.
     *
     * @param file is the file.
     * @return true if the file is js, otherwise false.
     */
    @Override
    public boolean accept(File file) {
        return file.getName().endsWith(INPUT_EXTENSION);
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
    public boolean fileDeleted(File file) throws WatchingException {
        process(file);
        return true;
    }
}
