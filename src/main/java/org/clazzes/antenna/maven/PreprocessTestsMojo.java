/***********************************************************
 * $Id: PreprocessTestsMojo.java 903 2013-10-04 16:01:28Z util $
 * 
 * org.clazzes Utils
 * http://www.clazzes.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 ***********************************************************/

package org.clazzes.antenna.maven;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * A MOJO, that executes the antenna preprocessor on the test source path of a project configuration.
 *
 * @goal preprocess-test
 */
public class PreprocessTestsMojo extends AbstractMojo {

    /**
     * The directory where to put the generated test java source files to.
     * This path is added of the project's source directories, so
     * the path must not contain the package subapths'. 
     * @required
     * @parameter default-value="${project.build.directory}/preprocessed-test-sources"
     */
    private File outputDirectory;

    /**
     * A list of additional test source directories to preprocess. This avoids the usage
     * of the build helper maven plugin which badly interacts with this plugin when
     * creating source attachments.
     * 
     * @parameter
     */
    private List<String> additionalTestSources;

    /**
     * The encoding of the Java source files.
     *  
     * @required
     * @parameter default-value="UTF-8"
     */
    private String sourceEncoding;

    /**
     * The list of preprocessor symbols. 
     * 
     * @required
     * @parameter
     */
    private List<String> symbols;

    /**
     * A list of package names to skip.
     * 
     * @parameter
     */
    private Set<String> skipPackages;

    /**
     * A list of class names to skip.
     * 
     * @parameter
     */
    private Set<String> skipClasses;

    /**
     * The maven project needed to modify the source paths. 
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    
    /* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@SuppressWarnings("unchecked")
	public void execute() throws MojoExecutionException, MojoFailureException {
	
		List<String> srcDirs = this.project.getTestCompileSourceRoots();

		SourceTreePreprocessor pp = new SourceTreePreprocessor();
	
		pp.setLog(this.getLog());
		pp.setSkipClasses(this.skipClasses);
		pp.setSkipPackages(this.skipPackages);
		pp.setSourceEncoding(this.sourceEncoding);
		pp.setSymbols(this.symbols);
		
		if (this.additionalTestSources != null) {
			
			srcDirs.addAll(this.additionalTestSources);
		}

		pp.preprocess(srcDirs,this.outputDirectory);
	}

}
