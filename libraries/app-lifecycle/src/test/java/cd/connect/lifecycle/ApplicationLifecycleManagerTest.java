package cd.connect.lifecycle;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class ApplicationLifecycleManagerTest {
  LifecycleTransition transition = null;
  @Test
  public void basicStatusChange() {
    ApplicationLifecycleManager.registerListener(trans -> {
      transition = trans;
    });
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTING);
    assertThat(ApplicationLifecycleManager.getStatus()).isEqualTo(LifecycleStatus.STARTING);
    assertThat(transition.next).isEqualTo(LifecycleStatus.STARTING);
    assertThat(transition.current).isEqualTo(LifecycleStatus.STARTING);
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTED);
    assertThat(ApplicationLifecycleManager.getStatus()).isEqualTo(LifecycleStatus.STARTED);
    assertThat(transition.next).isEqualTo(LifecycleStatus.STARTED);
    assertThat(transition.current).isEqualTo(LifecycleStatus.STARTING);
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.TERMINATING);
    assertThat(ApplicationLifecycleManager.getStatus()).isEqualTo(LifecycleStatus.TERMINATING);
    assertThat(transition.next).isEqualTo(LifecycleStatus.TERMINATING);
    assertThat(transition.current).isEqualTo(LifecycleStatus.STARTED);
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.TERMINATED);
    assertThat(ApplicationLifecycleManager.getStatus()).isEqualTo(LifecycleStatus.TERMINATED);
    assertThat(transition.next).isEqualTo(LifecycleStatus.TERMINATED);
    assertThat(transition.current).isEqualTo(LifecycleStatus.TERMINATING);
  }
}
