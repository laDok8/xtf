package cz.xtf.core.git;

import cz.xtf.core.config.XTFConfig;

/**
 * Get git URL set in XTF configuration
 * If not set, resolve to current repository in HTTPS format
 * In case there's multiple remotes for HEAD, prefer upstream or origin (in this order)
 *
 */
public class GitRemoteConfig {
    public static final String GIT_URL = "xtf.git.repository.url";
    public static final String GIT_BRANCH = "xtf.git.repository.ref";

    public static String getGitUrl() {
        String url = XTFConfig.get(GIT_URL, GitRemoteResolver.repositoryUrl());

        if (url == null) {
            throw new IllegalStateException("Unable to resolve git URL, specify " + GIT_URL);
        }
        return url;
    }

    public static String getGitRef() {
        String branch = XTFConfig.get(GIT_BRANCH, GitRemoteResolver.repositoryReference());

        if (branch == null) {
            throw new IllegalStateException("Unable to resolve git branch, specify " + GIT_BRANCH);
        }
        return branch;
    }
}
