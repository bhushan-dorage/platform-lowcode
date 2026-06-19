package com.platform.studio.artifact.service;

import com.platform.studio.artifact.domain.ArtifactType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Git-backed artifact store. Each tenant gets an isolated git repository.
 * Artifacts are versioned via git commits; published versions get git tags.
 */
@Slf4j
@Component
public class GitArtifactStore {

    @Value("${studio.git.repos-path:/var/platform/git-repos}")
    private String reposPath;

    /** Write artifact content to git working tree and commit. Returns commit SHA. */
    public String commit(String tenantId, ArtifactType type, String name, String content, String author) {
        try (Git git = openOrInit(tenantId)) {
            String filePath = filePath(type, name);
            Path file = repoDir(tenantId).resolve(filePath);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);

            git.add().addFilepattern(filePath).call();
            RevCommit commit = git.commit()
                    .setMessage("Save " + type.name() + ": " + name)
                    .setAuthor(author, author + "@platform.studio")
                    .call();
            log.debug("Committed artifact tenantId={} type={} name={} sha={}", tenantId, type, name, commit.getName());
            return commit.getName();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to commit artifact: " + e.getMessage(), e);
        }
    }

    /** Tag a specific commit as a named version (idempotent). */
    public void tag(String tenantId, ArtifactType type, String name, String version, String commitSha) {
        try (Git git = openOrInit(tenantId)) {
            String tag = tagName(type, name, version);
            try {
                ObjectId oid = git.getRepository().resolve(commitSha);
                RevCommit target = git.getRepository().parseCommit(oid);
                git.tag().setName(tag).setObjectId(target).setForceUpdate(true).call();
                log.debug("Tagged artifact tenantId={} tag={} sha={}", tenantId, tag, commitSha);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to tag artifact: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to open repo for tagging: " + e.getMessage(), e);
        }
    }

    /** Read artifact content at given ref (tag, commit SHA, or HEAD). */
    public String readContent(String tenantId, ArtifactType type, String name, String ref) {
        Path dir = repoDir(tenantId);
        if (!dir.resolve(".git").toFile().exists()) return null;
        try (Git git = Git.open(dir.toFile());
             Repository repo = git.getRepository();
             RevWalk revWalk = new RevWalk(repo)) {

            String resolveRef = ref != null ? ref : "HEAD";
            ObjectId objectId = repo.resolve(resolveRef);
            if (objectId == null) return null;
            RevCommit commit = revWalk.parseCommit(objectId);

            try (TreeWalk treeWalk = TreeWalk.forPath(repo, filePath(type, name), commit.getTree())) {
                if (treeWalk == null) return null;
                ObjectLoader loader = repo.open(treeWalk.getObjectId(0));
                return new String(loader.getBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Failed to read artifact tenantId={} type={} name={} ref={}", tenantId, type, name, ref, e);
            return null;
        }
    }

    /** Returns the tag name for a specific artifact version. */
    public String tagName(ArtifactType type, String name, String version) {
        return sanitize(name) + "_" + type.name().toLowerCase() + "_v" + version.replace(".", "_");
    }

    private String filePath(ArtifactType type, String name) {
        return type.dirName() + "/" + sanitize(name) + type.fileExtension();
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Path repoDir(String tenantId) {
        return Paths.get(reposPath, sanitize(tenantId));
    }

    private Git openOrInit(String tenantId) throws IOException, GitAPIException {
        Path dir = repoDir(tenantId);
        if (dir.resolve(".git").toFile().exists()) {
            return Git.open(dir.toFile());
        }
        Files.createDirectories(dir);
        Git git = Git.init()
                .setDirectory(dir.toFile())
                .setInitialBranch("main")
                .call();
        git.commit()
                .setMessage("init")
                .setAuthor("platform-studio", "studio@platform.io")
                .setAllowEmpty(true)
                .call();
        log.info("Initialized git repo for tenantId={} at {}", tenantId, dir);
        return git;
    }
}
