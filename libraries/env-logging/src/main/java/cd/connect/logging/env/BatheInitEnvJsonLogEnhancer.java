package cd.connect.logging.env;

import bathe.BatheInitializer;

/**
 * The purpose of this is to happen "after" the system properties are loaded. So we can init the env json log enhancer
 * (as long as it hasn't been loaded already).
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class BatheInitEnvJsonLogEnhancer implements BatheInitializer {
  @Override
  public int getOrder() {
    return 5;
  }

  @Override
  public String getName() {
    return "bathe-init-for-env-json-log-enhancer";
  }

  @Override
  public String[] initialize(String[] args, String jumpClass) {
    EnvJsonLogEnhancer.initialize();
    return args;
  }
}
