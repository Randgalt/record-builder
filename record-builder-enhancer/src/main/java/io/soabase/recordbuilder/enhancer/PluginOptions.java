/**
 * Copyright 2019 Jordan Zimmerman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.soabase.recordbuilder.enhancer;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;

@Command(name = "RecordBuilder Enhancer",
        description = "Enhances Java record class files with validations, preconditions, etc. See https://github.com/Randgalt/record-builder for details",
        usageHelpAutoWidth = true)
class PluginOptions {
    @Parameters(paramLabel = "DIRECTORY", arity = "0..1", description = "The build's output directory - i.e. where javac writes generated classes. The value can be a full path or a relative path. If not provided the Enhancer plugin will attempt to use standard directories.")
    String directory = "";

    @Option(names = {"-h", "--help"}, description = "Outputs this help")
    boolean helpRequested = false;

    @Option(names = {"-v", "--verbose"}, description = "Verbose output during compilation")
    boolean verbose = false;

    @Option(names = {"--disable"}, description = "Deactivate/disable the plugin")
    boolean disable = false;

    @Option(names = {"--dryRun"}, description = "Dry run only - doesn't modify any classes. You should enable verbose as well via: -v")
    boolean dryRun = false;

    @Option(names = {"--outputDirectory"}, description = "Optional alternate output directory for enhanced class files")
    File outputTo = null;
}
