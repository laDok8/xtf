package cz.xtf.junit5.extensions;

import cz.xtf.core.openshift.OpenShifts;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class CleanBeforeAllCallback implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    OpenShifts.master().clean().waitFor();
  }
}
