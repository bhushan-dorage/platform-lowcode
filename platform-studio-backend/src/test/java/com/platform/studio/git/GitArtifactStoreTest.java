package com.platform.studio.git;

import com.platform.studio.artifact.domain.ArtifactType;
import com.platform.studio.artifact.service.GitArtifactStore;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class GitArtifactStoreTest {

    @TempDir
    File tempDir;

    GitArtifactStore store;

    /**
     * JGit 6.x fails to parse gpg.format=ssh (only knows openpgp/x509).
     * Override SystemReader so the global ~/.gitconfig is never loaded.
     */
    @BeforeAll
    static void overrideGitSystemReader() {
        SystemReader.setInstance(new SystemReader() {
            @Override
            public String getHostname() { return "localhost"; }

            @Override
            public String getenv(String variable) { return System.getenv(variable); }

            @Override
            public String getProperty(String key) { return System.getProperty(key); }

            @Override
            public FileBasedConfig openUserConfig(Config parent, FS fs) {
                return new FileBasedConfig(parent, null, fs) {
                    @Override public void load() {}
                    @Override public boolean isOutdated() { return false; }
                };
            }

            @Override
            public FileBasedConfig openSystemConfig(Config parent, FS fs) {
                return new FileBasedConfig(parent, null, fs) {
                    @Override public void load() {}
                    @Override public boolean isOutdated() { return false; }
                };
            }

            @Override
            public FileBasedConfig openJGitConfig(Config parent, FS fs) {
                return new FileBasedConfig(parent, null, fs) {
                    @Override public void load() {}
                    @Override public boolean isOutdated() { return false; }
                };
            }

            @Override
            public long getCurrentTime() { return System.currentTimeMillis(); }

            @Override
            public int getTimezone(long when) {
                return java.util.TimeZone.getDefault().getOffset(when) / (60 * 1000);
            }
        });
    }

    @AfterAll
    static void restoreGitSystemReader() {
        SystemReader.setInstance(null);
    }

    @BeforeEach
    void setUp() throws Exception {
        store = new GitArtifactStore();
        Field f = GitArtifactStore.class.getDeclaredField("reposPath");
        f.setAccessible(true);
        f.set(store, tempDir.getAbsolutePath());
    }

    @Test
    void commit_createsGitRepoAndReturnsCommitSha() {
        String sha = store.commit("acme", ArtifactType.BPMN, "invoice-approval",
                "<bpmn>...</bpmn>", "alice");
        assertThat(sha).hasSize(40);
        assertThat(new File(tempDir, "acme/.git")).exists();
    }

    @Test
    void readContent_afterCommit_returnsStoredContent() {
        String content = "<?xml version=\"1.0\"?><bpmn>hello</bpmn>";
        String sha = store.commit("acme", ArtifactType.BPMN, "test-process", content, "alice");

        String read = store.readContent("acme", ArtifactType.BPMN, "test-process", sha);
        assertThat(read).isEqualTo(content);
    }

    @Test
    void readContent_fromHead_returnsLatestCommit() {
        store.commit("acme", ArtifactType.BPMN, "test-process", "v1", "alice");
        store.commit("acme", ArtifactType.BPMN, "test-process", "v2", "alice");

        String read = store.readContent("acme", ArtifactType.BPMN, "test-process", "HEAD");
        assertThat(read).isEqualTo("v2");
    }

    @Test
    void tag_thenReadByTag_returnsTaggedContent() {
        String sha = store.commit("acme", ArtifactType.BPMN, "invoice", "v1-content", "alice");
        store.tag("acme", ArtifactType.BPMN, "invoice", "1.0.0", sha);

        // commit a new version
        store.commit("acme", ArtifactType.BPMN, "invoice", "v2-content", "alice");

        // reading by tag still returns v1
        String tagName = store.tagName(ArtifactType.BPMN, "invoice", "1.0.0");
        String read = store.readContent("acme", ArtifactType.BPMN, "invoice", tagName);
        assertThat(read).isEqualTo("v1-content");
    }

    @Test
    void tenantIsolation_differentTenantsGetSeparateRepos() {
        store.commit("tenant-a", ArtifactType.FORM, "contact-form", "tenant-a-content", "alice");
        store.commit("tenant-b", ArtifactType.FORM, "contact-form", "tenant-b-content", "bob");

        String a = store.readContent("tenant-a", ArtifactType.FORM, "contact-form", "HEAD");
        String b = store.readContent("tenant-b", ArtifactType.FORM, "contact-form", "HEAD");
        assertThat(a).isEqualTo("tenant-a-content");
        assertThat(b).isEqualTo("tenant-b-content");
    }
}
