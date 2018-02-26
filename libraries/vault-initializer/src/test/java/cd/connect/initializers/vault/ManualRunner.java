package cd.connect.initializers.vault;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * Create a src/test/resources folder and take the ca.crt and token file from the pod
 * you are testing and put it in that directory.
 *
 * Created by Richard Vowles on 27/02/18.
 */
public class ManualRunner {
	private static void disableSslVerification() {
		try
		{
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[]{ new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		}};

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}
	// VAULT_TOKENFILE=src/test/resources/token
	// VAULT_CERTFILE=src/test/resources/ca.crt
	//
	public static void main(String[] args) {
		disableSslVerification();
		System.setProperty("fred", "[K8SVAULT]secret/apisecret");
		System.setProperty("vault.role", "api");
		System.setProperty("vault.url", "https://localhost:8200");
		System.setProperty("vault.tokenFile", "src/test/resources/token");
		System.setProperty("vault.certFile", "src/test/resources/ca.crt");
		new VaultInitializer().initialize(new String[] {}, null);
		System.out.println(System.getProperty("fred"));
	}
}
