package cd.connect.openapi;


import io.swagger.v3.oas.models.Operation;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.AbstractJavaJAXRSServerCodegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openapitools.codegen.utils.StringUtils.camelize;

public class Jersey2V3ApiGenerator extends AbstractJavaJAXRSServerCodegen implements CodegenConfig {
  private static final String LIBRARY_NAME = "jersey2-api";
  private static final String JERSEY2_TEMPLATE_FOLDER = "jersey2-v3template";
  private static final String SERVICE_ADDRESS = "serviceAddress";
  private static final String SERVICE_NAME = "serviceName";
  private static final String SERVICE_PORT = "servicePort";

  public Jersey2V3ApiGenerator() {
    super();
    library = LIBRARY_NAME;
    dateLibrary = "java8";
    supportedLibraries.clear();
    supportedLibraries.put(LIBRARY_NAME, LIBRARY_NAME);

    // tell the model about extra mustache files to generate

    // if we are using Kubernetes, we should get a service url. We separate these because the serviceName
    // is used for the Spring configuration class
    // this should appear in your config as:
    // <configOptions>
    //   <serviceName>...</serviceName> etc
    // </configOptions>
    cliOptions.add(new CliOption(SERVICE_NAME, "Name of service to use for @enable"));
    cliOptions.add(new CliOption(SERVICE_ADDRESS, "Name of service to use for @enable"));
    cliOptions.add(new CliOption(SERVICE_PORT, "Port of service to use for @enable"));
    cliOptions.add(new CliOption("suppressIgnoreUnknown", "Don't add the ignore unknown to the generated models"));



    // override the location
    embeddedTemplateDir = templateDir = JERSEY2_TEMPLATE_FOLDER;
  }

//  @Override
//  public List<CodegenArgument> getLanguageArguments() {
//    List<CodegenArgument> args = super.getLanguageArguments() == null ? new ArrayList<>() : new ArrayList<>(super.getLanguageArguments());
//    CodegenArgument e = new CodegenArgument();
//    e.setOption(CodegenConstants.API_TESTS_OPTION);
//    e.setValue("false");
//    args.add(e);
//    return args;
//  }

  public String getName() {
    return LIBRARY_NAME;
  }

  public String getHelp() {
    return "jersey2 api generator. generates all classes and interfaces with jax-rs annotations with jersey2 extensions as necessary";
  }

  // stoplight has a tendency to insert rubbish in the oas.json file
  @Override
  public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
    super.postProcessModelProperty(model, property);

    if("null".equals(property.example)) {
      property.example = null;
    }
  }

  /**
   * This gets called once we have been passed the configuration read in by the plugin.
   */
  @Override
  public void processOpts() {
    super.processOpts();

    apiTemplateFiles.remove("api.mustache");

    // no documentation, we're british
    modelDocTemplateFiles.clear();
    apiDocTemplateFiles.clear();

    modelTemplateFiles.put("model.mustache", ".java");

    if (additionalProperties.get("client") != null) {
      apiTemplateFiles.put("Impl.mustache", ".java");
      apiTemplateFiles.put("ClientService.mustache", ".java");
    }
    if (additionalProperties.get("server") != null) {
      apiTemplateFiles.put("Service.mustache", ".java");
    }
    if (additionalProperties.get("server-security") != null) {
      apiTemplateFiles.put("SecurityService.mustache", ".java");
    }

//    apiTemplateFiles.put("Configuration.mustache", ".java");

    // this is the name of the library and the date package we use

    apiTestTemplateFiles.clear();



    if (additionalProperties.get(SERVICE_NAME) != null) {
      String serviceName = additionalProperties.get(SERVICE_NAME).toString();
      if (additionalProperties.get(SERVICE_ADDRESS) != null) {
        addJersey2Client(serviceName, additionalProperties.get(SERVICE_ADDRESS).toString());
      } else if (additionalProperties.get(SERVICE_PORT) != null) {
        addJersey2Client(serviceName, String.format("%s-service:%s", serviceName, additionalProperties.get(SERVICE_PORT).toString()));
      }
    }

    if ( additionalProperties.containsKey(CodegenConstants.IMPL_FOLDER) ) {
      implFolder = (String) additionalProperties.get(CodegenConstants.IMPL_FOLDER);
    }
  }

  @Override
  public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> objs, List<Object> allModels) {
    List<CodegenOperation> codegenOperations = getCodegenOperations(objs);

    for (CodegenOperation op : codegenOperations) {
      // need to ensure the path if it has params as <> that it uses {} instead
      op.path = op.path.replace('<', '{').replace('>', '}');

      // an Object is actually a Response header. Ideally we don't ever want to return these but occasionally they
      // are required.
      if ("Object".equals(op.returnBaseType)) {
        op.returnBaseType = "Response";
      }
    }

    return super.postProcessOperationsWithModels(objs, allModels);
  }

  Map<String, CodegenModel> modelNames = new HashMap<>();

  @Override
  public Map<String, Object> postProcessModels(Map<String, Object> objs) {
    List<Map<String, Object>> models = (List<Map<String, Object>>) objs.get("models");
    models.stream().forEach(model -> {
      CodegenModel m = (CodegenModel) model.get("model");
      modelNames.put(m.classname, m);
    });
    return super.postProcessModels(objs);
  }
  
  @SuppressWarnings("unchecked")
  private List<CodegenOperation> getCodegenOperations(Map<String, Object> objs) {
    return (List<CodegenOperation>) getOperations(objs).get("operation");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object>getOperations(Map<String, Object> objs) {
    return (Map<String, Object>) objs.get("operations");
  }

  @Override
  public String toModelName(String name) {
    return (name != null) ? super.toModelName(name) : "<<unknown-to-model-name-is-null>>";
  }


  @Override
  public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co, Map<String, List<CodegenOperation>> operations) {
    String basePath = resourcePath;

    // ensure all paths are relative -> /customer/get ==> customer/get
    if (basePath.startsWith("/")) {
      basePath = basePath.substring(1);
    }

    // extracts the base of the reference -> customer/get => basePath = customer
    int pos = basePath.indexOf("/");
    if (pos > 0) {
      basePath = basePath.substring(0, pos);
    }

    // if the url is /customer originally, then basePath will be empty put it in the "default" group.
    //
    if (basePath.equals("")) {
      basePath = additionalProperties.get(SERVICE_NAME) == null ? "default" : additionalProperties.get(SERVICE_NAME).toString();
    } else {
      if (co.path.startsWith("/" + basePath)) {
        co.path = co.path.substring(("/" + basePath).length());
      }
      co.subresourceOperation = !co.path.isEmpty();
    }

    // we create a list we throw away???
    List<CodegenOperation> opList = operations.computeIfAbsent(basePath, k -> new ArrayList<CodegenOperation>());
    opList.add(co);
    co.baseName = basePath;
  }

  private void addJersey2Client(String serviceName, String serviceAddress) {
    System.out.printf("Service %s - located at `%s`\n", serviceName , serviceAddress);

    // standard Spring style naming
    String className = "Enable" + camelize(serviceName, false) + "Service";
    additionalProperties.put(SERVICE_NAME, className);
    additionalProperties.put(SERVICE_ADDRESS, serviceAddress);
    additionalProperties.put("package", modelPackage());

    supportingFiles.add(new SupportingFile("enable.mustache",
      sourceFolder + "/" + apiPackage().replace('.', '/'), className + ".java"));
  }

  @Override
  public String apiFilename(String templateName, String tag) {
    String suffix = (String)this.apiTemplateFiles().get(templateName);
    String result = this.apiFileFolder() + '/' + this.toApiFilename(tag) + suffix;
    int ix;
    if (templateName.endsWith("Impl.mustache")) {
      ix = result.lastIndexOf(47);
      result = result.substring(0, ix) + "/impl" + result.substring(ix, result.length() - 5) + "ServiceImpl.java";
    } else if (templateName.endsWith("Factory.mustache")) {
      ix = result.lastIndexOf(47);
      result = result.substring(0, ix) + "/factories" + result.substring(ix, result.length() - 5) + "ServiceFactory.java";
    } else if (templateName.endsWith("ClientService.mustache")) {
      ix = result.lastIndexOf(46);
      result = result.substring(0, ix) + "ClientService.java";
    } else if (templateName.endsWith("SecurityService.mustache")) {
      ix = result.lastIndexOf(46);
      result = result.substring(0, ix) + "SecuredService.java";
    } else if (templateName.endsWith("Service.mustache")) {
      ix = result.lastIndexOf(46);
      result = result.substring(0, ix) + "Service.java";
    }

    return result;
  }


  public String toApiName(String name) {
    if (additionalProperties.get(SERVICE_NAME) != null) {
      return additionalProperties.get(SERVICE_NAME).toString();
    }

    if (name.length() == 0) {
      return "DefaultApi";
    }

    return name.substring(0,1).toUpperCase() + name.substring(1);
  }
}
