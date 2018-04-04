package cd.connect.prescan;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.utils.ClasspathUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mojo(name = "prescan-config",
	defaultPhase = LifecyclePhase.PROCESS_CLASSES,
	requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class PrescanGeneratorMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", readonly = true)
	MavenProject project;

	@Parameter(defaultValue = "${project.build.directory}")
	File projectBuildDir;

	@Parameter(defaultValue = "lib")
	String jarpath;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Set<String> interesting;

		try {
			if (project != null) {
				URLClassLoader loader = makeClassLoaderFromProjectDependencies();

				interesting = scan(loader);
				if( !interesting.isEmpty() ){
					// make sure the META-INF folder is there
					File metaInfoFolder = new File(getOutputDirectoryFile(), "META-INF");
					metaInfoFolder.mkdirs();
					FileWriter writer = new FileWriter(new File(metaInfoFolder, "prescan"));
					for (String item : interesting) {
						writer.write(item + System.lineSeparator());
					}
					writer.flush();
				} else {
					getLog().info("No interesting web resources found?");
				}
			}
		} catch (Exception ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

	// overrideable for testing
	protected File getOutputDirectoryFile() {
		return new File(project.getBuild().getOutputDirectory());
	}

	protected Set<String> scan(ClassLoader loader) {
		Set<String> found = new HashSet<>();

		String outputDir = getOutputDirectoryFile().getAbsolutePath();

		FastClasspathScanner fcp = new FastClasspathScanner();
		fcp.addClassLoader(loader);
		fcp.matchFilenamePath("META-INF/web-fragment.xml", (classpathElt, relativePath, inputStream, isLength) -> {
			add(found, "fragment", classpathElt, relativePath, outputDir);
		});
		fcp.matchFilenamePattern("META-INF/resources/.*", (classpathElt, relativePath, inputStream, isLength) -> {
			add(found, "resource", classpathElt, "META-INF/resources/", outputDir);
		});
		fcp.matchFilenamePath("WEB-INF/web.xml", (classpathElt, relativePath, inputStream, isLength) -> {
				add(found, "webxml", classpathElt, relativePath, outputDir);
				add(found, "resource", classpathElt, "", outputDir);
		});
		fcp.matchFilenamePath("META-INF/resources/WEB-INF/web.xml", (classpathElt, relativePath, inputStream, isLength) -> {
			add(found, "webxml", classpathElt, relativePath, outputDir);
			add(found, "resource", classpathElt, "META-INF/resources/", outputDir);
		});
		fcp.scan();

		return found;
	}

	private String deferenceResource(String url, String outputDir) {
		if (url.contains("!")) { // jar:file:absolutePath!/offset
			String[] bits = url.split("!");
			String prefix = bits[0].substring(0, bits[0].indexOf("/") + 1);
			String jarfile = bits[0].substring(bits[0].lastIndexOf("/"));
			bits[0] = prefix + jarpath + jarfile;
			return String.join("!", bits);
		}

		if (url.contains(outputDir)) {
			url = url.replace(outputDir, "");
		}

		return "jar:" + url;
	}

	private void add(Set<String> found, String type, File classpathElt, String relativePath, String outputDir) {
		String path = deferenceResource(ClasspathUtils.getClasspathResourceURL(classpathElt, relativePath).toExternalForm(), outputDir);
		if (found.add(String.format("%s=%s", type, path))) {
			getLog().info(String.format("prescan-resource: %s -> `%s`", type, path));
		}

	}

	private URLClassLoader makeClassLoaderFromProjectDependencies() {
		List<URL> urls = new ArrayList<>();

		File outputDir = new File(project.getBuild().getOutputDirectory());

		if (outputDir.exists() && outputDir.isDirectory()) {
			try {
				urls.add(outputDir.toURI().toURL());

			} catch (MalformedURLException e) {
				getLog().error("Unable to get url for output folder: " + outputDir.getAbsolutePath());
			}
		}

		project.setArtifactFilter(artifact -> Artifact.SCOPE_COMPILE.equals(artifact.getScope()) || Artifact.SCOPE_RUNTIME.equals(artifact.getScope()));

		project.getArtifacts().forEach( artifact -> {
			try {
				urls.add(artifact.getFile().toURI().toURL());
			} catch (MalformedURLException e) {
				getLog().error("Unable to get url for dependency!", e);
			}
		});

		return new URLClassLoader(urls.toArray(new URL[0]), null);
	}

	private void add(List<String> found, String type, String path, URL origin) {
		getLog().info(String.format("prescan-resource: %s -> `%s` (origin `%s`)", type, path, origin.toExternalForm()));
		found.add(String.format("%s=%s", type, path));
	}

}
