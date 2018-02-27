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
 * take the ca.crt and token file from the pod
 * you are testing and put it in /tmp.
 *
 * Then port-forward to the vault pod.
 *
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

	public static void main(String[] args) {
//		disableSslVerification();
		System.setProperty("fred", "[K8SVAULT]secret/apisecret");
		System.setProperty("vault.role", "api");
		System.setProperty("vault.url", "https://vault.state.svc.cluster.local:8200");
		System.setProperty("vault.tokenFile", "/tmp/token");
		System.setProperty("vault.certFile", "/tmp/ca.crt");
		new VaultInitializer().initialize(new String[] {}, null);
		System.out.println(System.getProperty("fred"));
	}
}
