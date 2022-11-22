/***********************************************************
 * $Id: SourceTreePreprocessor.java 903 2013-10-04 16:01:28Z util $
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import antenna.preprocessor.v2.PPException;
import antenna.preprocessor.v2.Preprocessor;
import antenna.preprocessor.v2.Preprocessor.ILineFilter;
import antenna.preprocessor.v2.Preprocessor.ILogger;

/**
 * A class, which actually preprocesses a list of source trees.
 */
public class SourceTreePreprocessor {

	private Log log;
	
    /**
     * The encoding of the Java source files.
     */
    private String sourceEncoding;

    /**
     * The list of preprocessor symbols. 
     */
    private List<String> symbols;

    /**
     * A list of package names to skip.
     */
    private Set<String> skipPackages;

    /**
     * A list of class names to skip.
     */
    private Set<String> skipClasses;

    public String getSourceEncoding() {
		return sourceEncoding;
	}

	public void setSourceEncoding(String sourceEncoding) {
		this.sourceEncoding = sourceEncoding;
	}

	public List<String> getSymbols() {
		return symbols;
	}

	public void setSymbols(List<String> symbols) {
		this.symbols = symbols;
	}

	public Set<String> getSkipPackages() {
		return skipPackages;
	}

	public void setSkipPackages(Set<String> skipPackages) {
		this.skipPackages = skipPackages;
	}

	public Set<String> getSkipClasses() {
		return skipClasses;
	}

	public void setSkipClasses(Set<String> skipClasses) {
		this.skipClasses = skipClasses;
	}

	public void setLog(Log log) {
		this.log = log;
	}

	private Log getLog() {
    	return this.log;
    }
    
    private class AntennaLogger implements ILogger {

		public void log(String msg) {
			SourceTreePreprocessor.this.getLog().info(msg);
		}
    }
    
    private class AntennaLineFilter implements ILineFilter {

		public String filter(String line) {
			
			return line;
		}

    }
    
    private void preprocessSourceFile(Preprocessor pp, File src, File dest) throws IOException,PPException {
    	
    	if (this.getLog().isDebugEnabled())
    		this.getLog().debug("Preprocessing ["+src+"] to ["+dest+"].");
    	
    	pp.setFile(dest);
    	pp.preprocess(new FileInputStream(src),
    			      new FileOutputStream(dest),this.sourceEncoding);
    }
    
    private void preprocessOtherFile(Preprocessor pp, File src, File dest) throws IOException {
    	
    	if (this.getLog().isDebugEnabled())
    		this.getLog().debug("Copying ["+src+"] to ["+dest+"].");

    	byte [] buf = new byte[16384];
    	FileInputStream is = null;
    	FileOutputStream os = null;
    	
    	try {
    		is = new FileInputStream(src);
    		os = new FileOutputStream(dest);
    		int n;
    	
    		while ((n=is.read(buf)) > 0) {
    			os.write(buf,0,n);
    		}
    	} finally {
    		if (os != null)
    			os.close();
    		if (is != null)
        		is.close();
    	}
    }
    
    private void preprocessDirectory(Preprocessor pp, String pkg, File src, File dest) throws IOException, PPException {
   	
    	if (this.getLog().isDebugEnabled())
    		this.getLog().debug("Preprocessing subdir ["+src+"] to ["+dest+"].");
    	
    	String[] names = src.list();
    	
    	for (String name:names) {
    		
    		// skip .svn and the like...
    		if (name.startsWith(".")) continue;
    		
    		File srcFile = new File(src,name);
    		File destFile = new File(dest,name);
    		
    		if (destFile.exists() && !srcFile.isDirectory())
    			this.getLog().warn("The destination ["+destFile+"] already exists (duplicate source file?).");
    		
    		if (srcFile.isDirectory()) {
    			
    			String npkg;
    			if (pkg == null)
    				npkg = name;
    			else
    				npkg = pkg + "." + name;
    			

    			if (this.skipPackages != null && this.skipPackages.contains(npkg)) {
    			
    				if (this.getLog().isDebugEnabled())
    					this.getLog().debug("Skipping directory ["+srcFile+"].");
    				
    				continue;
    			}
    			
    			if (!destFile.exists() && !destFile.mkdir())
    				throw new IOException("Cannot create directory ["+destFile+"].");
    			
    			this.preprocessDirectory(pp,npkg,srcFile,destFile);
    		}
    		else if (name.endsWith(".java")) {
    			
    			String cls;
    			if (pkg == null)
    				cls = name;
    			else
    				cls = pkg + "." + name.substring(0,name.length()-5);
    			
    			if (this.skipClasses != null && this.skipClasses.contains(cls)) {
        			
    				if (this.getLog().isDebugEnabled())
    					this.getLog().debug("Skipping source file ["+srcFile+"].");
    				
    				continue;
    			}

    			this.preprocessSourceFile(pp,srcFile,destFile);
    		}
    		else {
    			this.preprocessOtherFile(pp,srcFile,destFile);
    		}
    	}
    }
    
    private static void rmdirRecursive(File dir) throws IOException {
    	
    	File[] files = dir.listFiles();
    	
    	for (File file : files) {
    		
    		if (file.isDirectory())
    			rmdirRecursive(file);
    		else {
    			
    	    	if (!file.delete())
    	    		throw new IOException("Cannot remove file ["+file+"] before preprocessing.");
    		}
    	}
    	
    	if (!dir.delete())
    		throw new IOException("Cannot remove directory ["+dir+"] before preprocessing.");
    }
    
    /**
     * Preprocess a list of source directories into the given output directory.
     * 
     * @param srcDirs A list of source directories to preprocess. After this
     *                subroutine has finished, this list is modified to only contain
     *                <code>outputDirectory</code>.
     * @param outputDirectory The directory to write the output to. This directory
     *               is recursively deleted and recreated, if it exists when entering
     *               this subroutine.
     * 
     * @throws MojoExecutionException Upon errors.
     */
    public void preprocess(List<String> srcDirs, File outputDirectory) throws MojoExecutionException {
	
		this.getLog().info("Preprocessing ["+srcDirs.size()+"] source paths to ["+outputDirectory+"]...");
				
		try {
			if (outputDirectory.exists())
				rmdirRecursive(outputDirectory);
			
			if (!outputDirectory.mkdirs())
				throw new IOException("Cannot create firectory ["+outputDirectory+"].");
			
		} catch (IOException e) {
			throw new MojoExecutionException("IOException preparing output directory ["+outputDirectory+"]",e);
		}
		
		for (String srcDir : srcDirs) {
		
			this.getLog().info("Preprocessing source path ["+srcDir+"]...");
	
			try {
				ILineFilter lineFilter = new AntennaLineFilter();
				ILogger logger = new AntennaLogger();
				Preprocessor pp = new Preprocessor(logger,lineFilter);
			
				for (String sym : this.symbols)
				{
					pp.addDefines(sym);
				}
				
				this.preprocessDirectory(pp,null,new File(srcDir),outputDirectory);
				
				this.getLog().info("Finished preprocessing source path ["+srcDir+"].");
			} catch (PPException e) {
				throw new MojoExecutionException("Exception preprocessing source path ["+srcDir+"]",e);
			} catch (IOException e) {
				throw new MojoExecutionException("I/O error preprocessing source path ["+srcDir+"]",e);
			}
		}
		this.getLog().info("Finished preprocessing ["+srcDirs.size()+"] source paths to ["+outputDirectory+"].");
		
		// completely replace the compiler path.
		srcDirs.clear();
		srcDirs.add(outputDirectory.getAbsolutePath());
	}

	/**
	 * Sets the DEBUG symbol for the antenna preprocessor that is used for
	 * #debug and #mdebug lines
	 * 
	 * @param level
	 *            the debuglevel. Supported values are
	 *            [debug|info|warn|error|fatal|none]
	 */
	public void setDebugLevel(String level) {
		// the level none and all unknown levels will not set a DEBUG value
		if (("debug".equalsIgnoreCase(level))
				|| ("info".equalsIgnoreCase(level))
				|| ("warn".equalsIgnoreCase(level))
				|| ("error".equalsIgnoreCase(level))
				|| ("fatal".equalsIgnoreCase(level))) {
			this.symbols.add("DEBUG=" + level);
		} else {
			// one might add a step that removes any "DEBUG=" entries from the list
			this.getLog()
					.error("Ignoring unsupported debug level "
							+ level
							+ ", Supported values are [debug|info|warn|error|fatal|none]");
		}
	}

}
