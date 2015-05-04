/*
 * Copyright 2013-2015 Alex Lin and rafale.
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
package com.opoopress.maven.plugins.archetype;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.ArchetypeGenerationResult;
import org.apache.maven.archetype.ui.generation.ArchetypeGenerationConfigurator;
import org.apache.maven.archetype.ui.generation.ArchetypeSelector;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author Alex Lin
 * @author rafale
 * @requiresProject false
 * @goal create
 */
public class CreateMojo extends AbstractMojo{
    /**
     * @component
     */
    private ArchetypeManager manager;

    /**
     * @component
     */
    private ArchetypeSelector selector;

    /**
     * @component
     */
    private ArchetypeGenerationConfigurator configurator;

    /**
     * @component
     */
    private Invoker invoker;

    /**
     * The archetype's artifactId.
     *
     * @parameter expression="${archetypeArtifactId}"
     */
    private String archetypeArtifactId;

    /**
     * The archetype's groupId.
     *
     * @parameter expression="${archetypeGroupId}"
     */
    private String archetypeGroupId;

    /**
     * The archetype's version.
     *
     * @parameter expression="${archetypeVersion}"
     */
    private String archetypeVersion;

    /**
     * The archetype's repository.
     *
     * @parameter expression="${archetypeRepository}"
     */
    private String archetypeRepository;

    /**
     * The archetype catalogs to use to build a list and let the user choose from.
     * It is a comma separated list of catalogs.
     * Catalogs use following schemes:
     * <ul>
     * <li>'<code>file://...</code>' with <code>archetype-catalog.xml</code> automatically appended when pointing to a directory</li>
     * <li>'<code>http://...</code>' or '<code>https://...</code>' with <code>archetype-catalog.xml</code> always appended</li>
     * <li>'<code>local</code>' which is the shortcut for '<code>file://~/.m2/archetype-catalog.xml</code>'</li>
     * <li>'<code>remote</code>' which is the shortcut for Maven Central repository, ie '<code>http://repo1.maven.org/maven2</code>'</li>
     * <li>'<code>internal</code>' which is an internal catalog</li>
     * <li>'<code>remote,local</code>' which is default value or maven archetype plugin 2.2.</li>
     * </ul>
     * <p/>
     *
     * @parameter expression="${archetypeCatalog}" default-value="http://repo.opoopress.com/releases/archetype-catalog.xml"
     */
    private String archetypeCatalog;

    /**
     * Local Maven repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * List of remote repositories used by the resolver.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    private List<ArtifactRepository> remoteArtifactRepositories;

    /**
     * User settings use to check the interactiveMode.
     *
     * @parameter expression="${interactiveMode}" default-value="${settings.interactiveMode}"
     * @required
     */
    private Boolean interactiveMode;

    /**
     * @parameter expression="${basedir}"
     */
    private File basedir;

    /**
     * @parameter expression="${session}"
     * @readonly
     */
    private MavenSession session;

    /**
     * Additional goals to immediately run on the project created from the archetype.
     *
     * @parameter expression="${goals}"
     */
    private String goals;

    /**
     * Applying some filter on displayed archetypes list: format is <code>artifactId</code> or <code>groupId:artifactId</code>.
     * <ul>
     * <li><code>org.apache:</code> -> displays all archetypes which contain org.apache in groupId</li>
     * <li><code>:jee</code> or <code>jee</code> -> displays all archetypes which contain jee in artifactId</li>
     * <li><code>org.apache:jee</code> -> displays all archetypes which contain org.apache in groupId AND jee in artifactId</li>
     * </ul>
     * Default value is ':opoopress-archetype-site'.
     *
     * @parameter expression="${filter}" default-value=":opoopress-archetype-site"
     */
    private String filter;

    /**
     * @parameter expression="${locale}"
     */
    private String locale;

    /**
     * @parameter expression="${theme.name}" default-value="default"
     */
    private String themeName;

    /**
     * @parameter expression="${theme.artifactId}" default-value="opoopress-theme-default"
     */
    private String themeArtifactId;

    /**
     * @parameter expression="${theme.version}" default-value="1.0-SNAPSHOT"
     */
    private String themeVersion;

    public void execute()
            throws MojoExecutionException, MojoFailureException {

        Properties executionProperties = session.getExecutionProperties();

        ArchetypeGenerationRequest request = new ArchetypeGenerationRequest()
                .setArchetypeGroupId(archetypeGroupId)
                .setArchetypeArtifactId(archetypeArtifactId)
                .setArchetypeVersion(archetypeVersion)
                .setOutputDirectory(basedir.getAbsolutePath())
                .setLocalRepository(localRepository)
                .setArchetypeRepository(archetypeRepository)
                .setRemoteArtifactRepositories(remoteArtifactRepositories)
                .setFilter(filter);

        try {
            if (interactiveMode.booleanValue()) {
                getLog().info("Generating project in Interactive mode");
            } else {
                getLog().info("Generating project in Batch mode");
            }

            selector.selectArchetype(request, interactiveMode, archetypeCatalog);

            if (StringUtils.isBlank(request.getArchetypeArtifactId())) {
                // no archetype found: stopping
                return;
            }

            configurator.configureArchetype(request, interactiveMode, executionProperties);

            ArchetypeGenerationResult generationResult = manager.generateProjectFromArchetype(request);

            if (generationResult.getCause() != null) {
                throw new MojoFailureException(generationResult.getCause(), generationResult.getCause().getMessage(),
                        generationResult.getCause().getMessage());
            }
        } catch (MojoFailureException ex) {
            throw ex;
        } catch (Exception ex) {
            throw (MojoFailureException) new MojoFailureException(ex.getMessage()).initCause(ex);
        }

        String artifactId = request.getArtifactId();

        //op:init,op:theme
        //initialize site
        Properties properties = new Properties();
        if(locale != null){
            properties.setProperty("locale", locale);
        }
        invokePostArchetypeGenerationGoals("op:init", artifactId, properties);

        //install theme
        if(themeName != null) {
            properties.setProperty("name", themeName);
        }
        if(themeArtifactId != null){
            properties.setProperty("artifactId", themeArtifactId);
        }
        if(themeVersion != null){
            properties.setProperty("version", themeVersion);
        }
        invokePostArchetypeGenerationGoals("op:theme", artifactId, properties);

        //additional goals
        String postArchetypeGenerationGoals = request.getArchetypeGoals();
        if (StringUtils.isEmpty(postArchetypeGenerationGoals)) {
            postArchetypeGenerationGoals = goals;
        }
        if (StringUtils.isNotEmpty(postArchetypeGenerationGoals)) {
            invokePostArchetypeGenerationGoals(postArchetypeGenerationGoals, artifactId, null);
        }
    }

    private void invokePostArchetypeGenerationGoals(String goals, String artifactId, Properties properties)
            throws MojoExecutionException, MojoFailureException {
        getLog().info("Invoking post-archetype-generation goals: " + goals);

        File projectBasedir = new File(basedir, artifactId);

        if (projectBasedir.exists()) {
            InvocationRequest request = new DefaultInvocationRequest()
                    .setBaseDirectory(projectBasedir)
                    .setGoals(Arrays.asList(StringUtils.split(goals, ",")));

            if(properties != null){
                request.setProperties(properties);
            }

            try {
                invoker.execute(request);
            } catch (MavenInvocationException e) {
                throw new MojoExecutionException("Cannot run additions goals.", e);
            }
        } else {
            getLog().info("Additional goals aborted: unavailable basedir " + projectBasedir);
        }
    }

    private Properties createProperties(String ... props){
        if (props.length == 0 || props.length % 2 != 0){
            throw new IllegalArgumentException();
        }
        Properties properties = new Properties();
        for(int i = 0 ; i < props.length ; i = i + 2){
            properties.setProperty(props[i], props[i+1]);
        }
        return properties;
    }
}
