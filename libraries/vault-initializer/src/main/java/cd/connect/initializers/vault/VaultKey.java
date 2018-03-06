package cd.connect.initializers.vault;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Richard Vowles on 6/03/18.
 */
public class VaultKey {
	// I'm with Steve Yegge on this one
	public final String systemPropertyFieldName;
	public final String vaultKeyName;
	public Map<String, String> subPropertyFieldNames = new HashMap<>();

	public String getSystemPropertyFieldName() {
		return systemPropertyFieldName;
	}

	public String getVaultKeyName() {
		return vaultKeyName;
	}

	public VaultKey(String systemPropertyFieldName, String vaultKeyName) {
		this.systemPropertyFieldName = systemPropertyFieldName;
		this.vaultKeyName = split(vaultKeyName);
	}

	private String split(String systemPropertyFieldName) {
		String[] parts = systemPropertyFieldName.split("#");

		if (parts.length > 1) {
			List<String> subParts = Arrays.stream(parts[1].split(","))
				.map(String::trim)
				.filter(s -> s.length() > 0)
				.collect(Collectors.toList());

			// check to see if they are name=value or name:value pairs, if so, store them as such
			subParts.forEach(subPart -> {
				String[] mapped = subPart.split("[:=]");
				if (mapped.length == 2) {
					subPropertyFieldNames.put(mapped[0], mapped[1]);
				} else {
					subPropertyFieldNames.put(mapped[0], mapped[0]);
				}
			});
		}

		return parts[0];
	}
}
