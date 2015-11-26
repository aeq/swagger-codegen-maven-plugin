package com.wordnik.swagger.codegen.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
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
 */

import config.Config;
import config.ConfigParser;
import io.swagger.codegen.*;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ServiceLoader;

import static com.wordnik.swagger.codegen.plugin.AdditionalParams.TEMPLATE_DIR_PARAM;

/**
 * Goal which generates client/server code from a swagger json/yaml definition.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodeGenMojo extends AbstractMojo {

    /**
     * Location of the output directory.
     */
    @Parameter(name = "output",
            property = "swagger.codegen.maven.plugin.output",
            defaultValue = "${project.build.directory}/generated-sources/swagger")
    private File output;

    /**
     * Location of the swagger spec, as URL or file.
     */
    @Parameter(name = "inputSpec", required = true)
    private String inputSpec;

    /**
     * Folder containing the template files.
     */
    @Parameter(name = "templateDirectory")
    private File templateDirectory;

    /**
     * Client language to generate.
     */
    @Parameter(name = "language", required = true)
    private String language;


    /**
     * Add the output directory to the project as a source root, so that the
     * generated java types are compiled and included in the project artifact.
     */
    @Parameter(defaultValue = "true")
    private boolean addCompileSourceRoot = true;

    /**
     * Location for a configuration file for customisation of the swagger client code generation
     *
     */
    @Parameter(name = "configFile")
    private String configFile;

    /**
     * The project being built.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        Swagger swagger = new SwaggerParser().read(inputSpec);

        CodegenConfig config = forName(language);
        config.setOutputDir(output.getAbsolutePath());

        if (null != templateDirectory) {
            config.additionalProperties().put(TEMPLATE_DIR_PARAM, templateDirectory.getAbsolutePath());
        }

        ClientOptInput input = new ClientOptInput().opts(new ClientOpts()).swagger(swagger);
        if (null != configFile) {
            applyConfigFileSettings(config);
        }

        input.setConfig(config);
        new DefaultGenerator().opts(input).generate();

        if (addCompileSourceRoot) {
            project.addCompileSourceRoot(output.toString());
        }
    }

    private void applyConfigFileSettings(final CodegenConfig config) {
        Config genConfig = ConfigParser.read(configFile);
        if (null != genConfig) {
            for (CliOption langCliOption : config.cliOptions()) {
                if (genConfig.hasOption(langCliOption.getOpt())) {
                    config.additionalProperties().put(langCliOption.getOpt(), genConfig.getOption(langCliOption.getOpt()));
                }
            }
        }
    }

    private CodegenConfig forName(String name) {
System.out.println("0=====search config:" + name);           
        ServiceLoader<CodegenConfig> loader = ServiceLoader.load(CodegenConfig.class);
        for (CodegenConfig config : loader) {
            if (config.getName().equals(name)) {
System.out.println("1=====found config:" + config);                
                return config;
            }
        }

        // else try to load directly
        try {
System.out.println("2=====search config by class:" + name);             
            return (CodegenConfig) Class.forName(name).newInstance();
        } catch (Exception e) {
System.out.println("3=====config not found:" + name);              
            throw new RuntimeException("Can't load config class with name ".concat(name), e);
        }
    }
}
