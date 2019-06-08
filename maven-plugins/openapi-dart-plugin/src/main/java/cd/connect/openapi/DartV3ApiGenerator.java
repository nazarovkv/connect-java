package cd.connect.openapi;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.DartClientCodegen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DartV3ApiGenerator extends DartClientCodegen implements CodegenConfig {
  private static final Logger log = LoggerFactory.getLogger(DartV3ApiGenerator.class);
	private static final String LIBRARY_NAME = "dart2-api";
	private static final String DART2_TEMPLATE_FOLDER = "dart2-v3template";

	public DartV3ApiGenerator() {
		super();
		library = LIBRARY_NAME;
		supportedLibraries.clear();
		supportedLibraries.put(LIBRARY_NAME, LIBRARY_NAME);
	}

	public String getName() {
		return LIBRARY_NAME;
	}

	public String getHelp() {
		return "dart2 api generator. generates all classes and interfaces with jax-rs annotations with jersey2 extensions as necessary";
	}

  @Override
  public void processOpts() {
    additionalProperties.put(SUPPORT_DART2, Boolean.TRUE);

    super.processOpts();

    // override the location
    embeddedTemplateDir = templateDir = DART2_TEMPLATE_FOLDER;

    List<SupportingFile> removeBad = supportingFiles.stream().filter(sf -> {
      return sf.templateFile.contains("git_push.sh") || sf.templateFile.contains("gitignore") || sf.templateFile.contains("README") || sf.templateFile.contains("travis");
    }).collect(Collectors.toList());

    supportingFiles.removeAll(removeBad);
  }

  // don't just add stuff to the end of the name willy nilly, check it is a reserved word first
  @Override
  public String escapeReservedWord(String name) {
	  return (isReservedWord(name)) ? name + "_" : name;
  }


  // existing one is unpleasant to use.
  @Override
  public String toEnumVarName(String value, String datatype) {
    if (value.length() == 0) {
      return "empty";
    }
    String var = value;
    if ("number".equalsIgnoreCase(datatype) ||
      "int".equalsIgnoreCase(datatype)) {
      var = "Number" + var;
    }
    return escapeReservedWord(var);
  }

  // if $FLUTTER is set, format the file.
  @Override
  public void postProcessFile(File file, String fileType) {
    String flutterDir = System.getenv("FLUTTER ");

    if (file == null || flutterDir == null) {
      return;
    }

    String dartPostProcessFile = String.format("%s/bin/cache/dart-sdk/bin/dartfmt -w", flutterDir);

    if (StringUtils.isEmpty(dartPostProcessFile)) {
      return; // skip if DART_POST_PROCESS_FILE env variable is not defined
    }

    // only process files with dart extension
    if ("dart".equals(FilenameUtils.getExtension(file.toString()))) {
      String command = dartPostProcessFile + " " + file.toString();
      try {
        Process p = Runtime.getRuntime().exec(command);
        int exitValue = p.waitFor();
        if (exitValue != 0) {
          log.error("Error running the command ({}). Exit code: {}", command, exitValue);
        } else {
          log.info("Successfully executed: " + command);
        }
      } catch (Exception e) {
        log.error("Error running the command ({}). Exception: {}", command, e.getMessage());
      }
    }
  }
}
