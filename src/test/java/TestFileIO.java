/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.activeviam.UnsafeUtil;
import com.activeviam.platform.LinuxPlatform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

import static org.assertj.core.api.Assertions.assertThat;

public class TestFileIO {

    private final LinuxPlatform PLATFORM = LinuxPlatform.getInstance();
    private final sun.misc.Unsafe UNSAFE = UnsafeUtil.getUnsafe();

    @TempDir
    static Path tempDir;

    @Test
    void testReadWrite(@TempDir Path tempDir) {
        System.out.println("Running test with temp dir " + tempDir);

//        try {
//            final Path path = tempDir.resolve("zob");
//
//            File file = path.toFile();
//            boolean newFile = file.createNewFile();
//            file.deleteOnExit();
            final int fd = PLATFORM.openFile("file");
            assertThat(fd).isGreaterThan(0);

            final long ptr = UNSAFE.allocateMemory(16);
            UNSAFE.putDouble(ptr, 42);
            UNSAFE.putDouble(ptr + 8, 42);

            PLATFORM.writeToFile(fd, ptr, 16);
            PLATFORM.readFromFile(fd, ptr, 16);

            assertThat(UNSAFE.getDouble(ptr)).isEqualTo(42);
            assertThat(UNSAFE.getDouble(ptr + 8)).isEqualTo(42);

//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

}