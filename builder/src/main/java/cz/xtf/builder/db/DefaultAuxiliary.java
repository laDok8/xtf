package cz.xtf.builder.db;

public abstract class DefaultAuxiliary implements OpenShiftAuxiliary {
    protected final String symbolicName;

    public DefaultAuxiliary(final String symbolicName) {
        this.symbolicName = symbolicName;
    }

    public abstract String getDeploymentConfigName();
}
