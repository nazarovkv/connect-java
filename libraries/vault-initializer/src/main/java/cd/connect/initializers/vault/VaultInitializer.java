package cd.connect.initializers.vault;

import bathe.BatheInitializer;
import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class VaultInitializer implements BatheInitializer {
  public static final String VAULT_KEY_PREFIX = "[K8SVAULT]";
  private Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public int getOrder() {
    return 1; // very important to do this early
  }

  @Override
  public String getName() {
    return "k8s-vault-initializer";
  }

  @Override
  public String[] initialize(String[] args, String jumpClass) {
    List<VaultKey> vaultKeys = discoverFields();

    if (vaultKeys.size() == 0) {
      log.info("vault: no keys in system properties to discover");
    } else {
      loadVaultKeys(vaultKeys);
    }
    
    return args;
  }

  static String readFile(String path, Charset encoding) {
    byte[] encoded = new byte[0];
    try {
      encoded = Files.readAllBytes(Paths.get(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new String(encoded, encoding);
  }

  private void loadVaultKeys(List<VaultKey> vaultKeys) {
    String vaultServer = System.getProperty("vault.url", "vault-server");

    if (vaultServer == null) {
      throw new RuntimeException("Vault keys were discovered but we have no Vault Server.");
    }

    String vaultJwtTokenFile = System.getProperty("vault.tokenFile", "/var/run/secrets/kubernetes.io/serviceaccount/token");
    String vaultCertFile = System.getProperty("vault.certFile", "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt");
    
    try {
      final VaultConfig config =
        new VaultConfig()
          .address(vaultServer)               // Defaults to "VAULT_ADDR" environment variable
        .token(readFile(vaultJwtTokenFile, Charset.forName("UTF-8")))  // Defaults to "VAULT_TOKEN" environment variable
        .openTimeout(5)                                 // Defaults to "VAULT_OPEN_TIMEOUT" environment variable
        .readTimeout(30)                                // Defaults to "VAULT_READ_TIMEOUT" environment variable
        .sslConfig(new SslConfig().pemFile(new File(vaultCertFile)).build())             // See "SSL Config" section below
        .build();

      final Vault vault = new Vault(config);

      vaultKeys.parallelStream().forEach(vaultKey -> {
        try {
          final String val = vault.logical().read(vaultKey.getVaultKeyName()).getData().get("value");
          System.setProperty(vaultKey.getSystemPropertyFieldName(), val);
          log.info("vault: set key {} from vault.", vaultKey.getSystemPropertyFieldName());
        } catch (VaultException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (VaultException e) {
      throw new RuntimeException(e);
    }
  }

  class VaultKey {
    private final String systemPropertyFieldName;
    private final String vaultKeyName;

    public String getSystemPropertyFieldName() {
      return systemPropertyFieldName;
    }

    public String getVaultKeyName() {
      return vaultKeyName;
    }

    public VaultKey(String systemPropertyFieldName, String vaultKeyName) {
      this.systemPropertyFieldName = systemPropertyFieldName;
      this.vaultKeyName = vaultKeyName;
    }
  }

  List<VaultKey> discoverFields() {
    List<VaultKey> vaultKeys = new ArrayList<>();

    System.getProperties().entrySet().forEach( entry -> {
      String key = entry.getKey().toString();
      String value = entry.getValue().toString();

      if (value.startsWith(VAULT_KEY_PREFIX)) {
        String part = value.substring(VAULT_KEY_PREFIX.length()).trim();

        log.debug("vault key: {} looking for {}", key, part);
        vaultKeys.add(new VaultKey(key, part));
      }
    });

    return vaultKeys;
  }


}
