package cd.connect.initializers.vault;

import bathe.BatheInitializer;
import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.rest.Rest;
import com.bettercloud.vault.rest.RestResponse;
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
      try {
        Vault vault = configureVaultClient();

        loadVaultKeys(vault, vaultKeys);
      } catch (VaultException e) {
        throw new RuntimeException(e);
      }


    }
    
    return args;
  }

  static String readFile(String path, Charset encoding) {
    byte[] encoded;
    try {
      encoded = Files.readAllBytes(Paths.get(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new String(encoded, encoding);
  }

  private String getenv(String env, String def) {
    String val = System.getenv(env);
    return val == null ? def : val;
  }

  private void loadVaultKeys(Vault vault, List<VaultKey> vaultKeys) {
      vaultKeys.parallelStream().forEach(vaultKey -> {
        try {
          final String val = vault.logical().read(vaultKey.getVaultKeyName()).getData().get("value");
          System.setProperty(vaultKey.getSystemPropertyFieldName(), val);
          log.info("vault: set key {} from vault.", vaultKey.getSystemPropertyFieldName());
        } catch (VaultException e) {
          throw new RuntimeException(e);
        }
      });
  }

  private Vault configureVaultClient() throws VaultException {
    String vaultServer = System.getProperty("vault.url", "vault-server");

    if (vaultServer == null) {
      throw new RuntimeException("Vault keys were discovered but we have no Vault Server.");
    }

    String vaultJwtTokenFile = getenv("VAULT_TOKENFILE", "/var/run/secrets/kubernetes.io/serviceaccount/token");
    String vaultCertFile = getenv("VAULT_CERTFILE", "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt");
    String vaultRole = System.getProperty("vault.role", System.getProperty("app.name"));
    String vaultJwtToken = System.getenv("VAULT_TOKEN");

    if (vaultJwtToken == null) {
      vaultJwtToken = readFile(vaultJwtTokenFile, Charset.forName("UTF-8"));
    }

    final VaultConfig config =
      new VaultConfig()
        .address(vaultServer)               // Defaults to "VAULT_ADDR" environment variable
        .openTimeout(5)                                 // Defaults to "VAULT_OPEN_TIMEOUT" environment variable
        .readTimeout(30)                                // Defaults to "VAULT_READ_TIMEOUT" environment variable
        .sslConfig(new SslConfig().pemFile(new File(vaultCertFile)).build())             // See "SSL Config" section below
        .build();

    if (vaultRole != null) {
      config.token(requestAuthTokenByRole(config, vaultJwtToken, vaultRole).getAuthClientToken());
    } else {
      config.token(vaultJwtToken);
    }

    return new Vault(config);
  }

  /**
   * this is the pattern from BetterCloud's library. They just don't support role or kubernetes login. They also don't
   * support most of the returned info.
   *
   * @param config - the Vault Config
   * @param vaultJwtToken - the JWT used to make the auth request (to get the real token)
   * @param vaultRole - the role we are
   *
   * @return - an object decoding some of the payload, but importantly, the auth client token
   */
  private AuthResponse requestAuthTokenByRole(VaultConfig config, String vaultJwtToken, String vaultRole) {
    int retryCount = 0;
    while (true) {
      try {
        // HTTP request to Vault
        final String requestJson = Json.object().add("jwt", vaultJwtToken).add("role", vaultRole).toString();
        final RestResponse restResponse = new Rest()//NOPMD
          .url(config.getAddress() + getenv("VAULT_ROLE_URL", "/v1/auth/kubernetes/login"))
          .body(requestJson.getBytes("UTF-8"))
          .connectTimeoutSeconds(config.getOpenTimeout())
          .readTimeoutSeconds(config.getReadTimeout())
          .sslVerification(config.getSslConfig().isVerify())
          .sslContext(config.getSslConfig().getSslContext())
          .post();

        // Validate restResponse
        if (restResponse.getStatus() != 200) {
          throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
        }
        final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
        if (!mimeType.equals("application/json")) {
          throw new VaultException("Vault responded with MIME type: " + mimeType, restResponse.getStatus());
        }
        return new AuthResponse(restResponse, retryCount);
      } catch (Exception e) {
        // If there are retries to perform, then pause for the configured interval and then execute the loop again...
        if (retryCount < config.getMaxRetries()) {
          retryCount++;
          try {
            final int retryIntervalMilliseconds = config.getRetryIntervalMilliseconds();
            Thread.sleep(retryIntervalMilliseconds);
          } catch (InterruptedException e1) {
            e1.printStackTrace();
          }
        } else {
          throw new RuntimeException(e);
        }
      }
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
