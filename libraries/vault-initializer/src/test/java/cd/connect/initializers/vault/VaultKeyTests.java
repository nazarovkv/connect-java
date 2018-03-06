package cd.connect.initializers.vault;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by Richard Vowles on 6/03/18.
 */
public class VaultKeyTests {
	@Test
	public void basicVaultKeyTests() {
		VaultKey vk = new VaultKey("fred", "/my/key#name=value,name2:value2");
		assertThat(vk.systemPropertyFieldName).isEqualTo("fred");
		assertThat(vk.vaultKeyName).isEqualTo("/my/key");
		assertThat(vk.subPropertyFieldNames.size()).isEqualTo(2);
		assertThat(vk.subPropertyFieldNames.get("name")).isEqualTo("value");
		assertThat(vk.subPropertyFieldNames.get("name2")).isEqualTo("value2");
	}
}
