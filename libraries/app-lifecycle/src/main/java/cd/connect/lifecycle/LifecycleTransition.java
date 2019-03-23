package cd.connect.lifecycle;

/**
 * Allows us to track the change of state
 *
 * * by Richard Vowles (https://www.google.com/+RichardVowles)
 */
public class LifecycleTransition {
	public final LifecycleStatus current;
	public final LifecycleStatus next;

	public LifecycleTransition(LifecycleStatus current, LifecycleStatus next) {
		this.current = current;
		this.next = next;
	}
}
