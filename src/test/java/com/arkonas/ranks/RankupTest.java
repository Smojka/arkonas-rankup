package com.arkonas.ranks;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import com.arkonas.ranks.hook.GroupProvider;
import com.arkonas.ranks.providers.TestEconomyProvider;
import com.arkonas.ranks.providers.TestGroupProvider;
import com.arkonas.ranks.providers.TestPermissionManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public abstract class RankupTest {
    private final File testResourceFolder;

    public RankupTest() {
        this("default");
    }

    public RankupTest(String testResourceFolder) {
        URL resource = this.getClass().getResource("/" + testResourceFolder);
        if (resource != null) {
            this.testResourceFolder = new File(resource.getPath());
        } else {
            this.testResourceFolder = null;
        }
    }

    protected GroupProvider groupProvider;
    protected ServerMock server;
    protected ArkonasRanksPlugin plugin;

    @BeforeEach
    public void setup() {
        System.setProperty("RANKUP_TEST", "true");

        try {
            groupProvider = new TestGroupProvider();

            server = MockBukkit.mock();
            plugin = (ArkonasRanksPlugin) server.getPluginManager()
                .loadPlugin(ArkonasRanksPlugin.class, new Object[]{
                    new TestPermissionManager(groupProvider),
                    new TestEconomyProvider()
                });

            if (this.testResourceFolder != null) {
                Path testPath = this.testResourceFolder.toPath();
                Path pluginPath = plugin.getDataFolder().toPath();
                Files.walkFileTree(testPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                        Path out = pluginPath.resolve(testPath.relativize(file));
                        System.out.println("Copy " + file + " to " + out);
                        out.getParent().toFile().mkdirs();
                        Files.copy(file, out);
                        return super.visitFile(file, attrs);
                    }
                });
            }

            server.getPluginManager().enablePlugin(plugin);

            // let rankup finish setting up
            server.getScheduler().performTicks(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
        System.clearProperty("RANKUP_TEST");
    }
}
