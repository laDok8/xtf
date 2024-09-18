package cz.xtf.core.git;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Try to resolve repository remote URL and branch from .git directory
 * <p>
 * This method tries to match HEAD commit to remote references.
 * If there is a match, the remote URL and branch are set.
 * For attached HEAD this method could be simplified with jgit methods, but
 * "universal" approach was chosen, as for example Jenkins git plugin creates a detached state.
 * In case of multiple matches, upstream or origin (in this order) is preferred.
 * </p>
 */
@Slf4j
class GitRemoteResolver {
    private static final String URL_TEMPLATE = "https://%s/%s/%s";
    private static String reference;
    private static String url;

    /**
     * We require HTTPS format, for unauthorized access to the repository, let's convert it
     */
    private static String getRepositoryUrl(String host, String remote, String repository) {
        return String.format(URL_TEMPLATE, host, remote, repository);
    }

    /**
     * Try to set repository ref and URL from HEAD commit
     * </p>
     * 
     * @param gitDir Path to .git directory
     */
    private static void resolveRepoFromHEAD(Optional<File> gitDir) {
        if (!gitDir.isPresent()) {
            log.debug("Failed to find a git config");
            return;
        }

        try (Git git = Git.open(gitDir.get())) {
            Repository repository = git.getRepository();

            //get current commit hash
            ObjectId commitId = repository.resolve("HEAD");

            //get all remote references
            List<Ref> refs = repository.getRefDatabase().getRefs().stream()
                    .filter(reference -> reference.getName().startsWith("refs/remotes/"))
                    .collect(Collectors.toList());

            List<String> matches = new ArrayList<>();
            // Walk through all the refs to see if any point to this commit
            for (Ref ref : refs) {
                if (ref.getObjectId().equals(commitId)) {
                    matches.add(ref.getName());
                }
            }

            if (matches.isEmpty()) {
                log.debug("No remote references found for the current commit");
                return;
            }

            //In case there are multiple matches, we prefer upstream or origin (in this order)
            List<String> preferredMatches = matches.stream()
                    .filter(reference -> reference.contains("upstream") || reference.contains("origin"))
                    .sorted(Comparator.reverseOrder()) // 1) upstream 2) origin
                    .collect(Collectors.toList());

            if (matches.size() > 1 && !preferredMatches.isEmpty()) {
                matches = preferredMatches;
            }

            //branch is string behind the last /
            reference = matches.stream()
                    .findFirst()
                    .map(ref -> ref.substring(ref.lastIndexOf('/') + 1))
                    .orElse(null);

            log.info("xtf.git.repository.ref got automatically resolved as {}", reference);

            String remote = repository.getRemoteName(matches.get(0));
            url = getRemoteUrl(repository, remote);

            if (url != null) {
                log.info("xtf.git.repository.url got automatically resolved as {}", url);
            }

        } catch (IOException | URISyntaxException e) {
            log.debug("Failed to dynamically set the tested repository url and reference", e);
        }
    }

    /**
     * given a remote reference, get it's remote URL
     * 
     * @param repository git repository
     * @param remoteReference reference in format "refs/remotes/remote/branch"
     * @return URL in HTTPS format
     */
    private static String getRemoteUrl(Repository repository, String remoteReference) throws URISyntaxException {
        RemoteConfig remoteConfig = new RemoteConfig(repository.getConfig(), remoteReference);
        if (remoteConfig.getURIs() == null || remoteConfig.getURIs().isEmpty()) {
            log.info("Missing URI in git remote ref '{}'", remoteReference);
            return null;
        }
        // we expect a single URI
        String[] pathTokens = remoteConfig.getURIs().get(0).getPath().split("/");
        if (pathTokens.length != 2) {
            log.info("Unexpected path '{}' in URI '{}' of git remote ref '{}'",
                    remoteConfig.getURIs().get(0).getPath(), remoteConfig.getURIs().get(0), remoteReference);
            return null;
        }
        // the URI must be in HTTPS format
        return getRepositoryUrl(remoteConfig.getURIs().get(0).getHost(), pathTokens[0], pathTokens[1]);
    }

    static {
        Optional<File> gitDir = findGit();
        resolveRepoFromHEAD(gitDir);
    }

    /**
     * look for a git repository recursively till system root folder
     * 
     * @return .git directory
     */
    private static Optional<File> findGit() {
        Path current = Paths.get(".").normalize().toAbsolutePath().normalize();
        // what is here? (previously limited to 3 hops)
        while (current != current.getParent()) {
            // look into a parent directory
            File[] gitConfig = current.toFile().listFiles((dir, name) -> name.equals(".git"));
            if (gitConfig != null && gitConfig.length == 1) {
                return Optional.of(gitConfig[0]);
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    protected static String repositoryReference() {
        return reference;
    }

    protected static String repositoryUrl() {
        return url;
    }
}
