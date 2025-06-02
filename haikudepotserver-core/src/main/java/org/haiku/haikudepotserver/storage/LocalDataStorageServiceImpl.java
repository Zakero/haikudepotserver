/*
 * Copyright 2018-2025, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haiku.haikudepotserver.storage;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.haiku.haikudepotserver.storage.model.DataStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>An implementation of the {@link DataStorageService}
 * which stores job data to a local temporary directory.</p>
 */

public class LocalDataStorageServiceImpl implements DataStorageService {

    protected static final Logger LOGGER = LoggerFactory.getLogger(LocalDataStorageServiceImpl.class);

    private final static String PATH_APPTMPDIR = "haikudepotserver-data";

    private File tmpDir;

    private final Boolean isProduction;

    private final Clock clock = Clock.systemUTC();

    public LocalDataStorageServiceImpl(Boolean isProduction) {
        this.isProduction = isProduction;
    }

    @PostConstruct
    public void init() {
        String platformTmpDirPath = System.getProperty("java.io.tmpdir");

        if (Strings.isNullOrEmpty(platformTmpDirPath)) {
            throw new IllegalStateException("unable to ascertain the java temporary directory");
        }

        tmpDir = new File(
                platformTmpDirPath,
                PATH_APPTMPDIR + (null == isProduction || !isProduction ? "-test" : ""));

        if (!tmpDir.exists()) {
            if (tmpDir.mkdirs()) {
               LOGGER.info("did create the application temporary directory path; {}", tmpDir.getAbsolutePath());
            }
            else {
                throw new IllegalStateException("unable to create the application temporary directory path; " + tmpDir.getAbsolutePath());
            }
        }
        else {
            LOGGER.info(
                    "{} files already exist in the application temporary directory; {}",
                    tmpDir.list().length,
                    tmpDir.getAbsolutePath());
        }

    }

    private File fileForKey(String key) {
       return new File(tmpDir, key + ".dat");
    }

    @Override
    public Set<String> keys(Duration olderThanDuration) {
        File[] leaves = tmpDir.listFiles();

        if (null == leaves) {
            return Set.of();
        }

        return Arrays.stream(leaves)
                .filter(f -> null == olderThanDuration || f.lastModified() < (clock.millis() - olderThanDuration.toMillis()))
                .map(f -> StringUtils.stripEnd(f.getName(), ".dat"))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public ByteSink put(final String key) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
        return Files.asByteSink(fileForKey(key));
    }

    @Override
    public Optional<? extends ByteSource> get(final String key) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
        File file = fileForKey(key);
        return file.exists() ? Optional.of(Files.asByteSource(file)) : Optional.empty();
    }

    @Override
    public boolean remove(String key) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
        File f = fileForKey(key);
        return !f.exists() || f.delete();
    }

    @Override
    public void clear() {
        LOGGER.info("will clear");

        for (File f : tmpDir.listFiles()) {
            if (f.isFile()) {
                if (f.delete()) {
                    LOGGER.info("did delete; {}", f.getAbsolutePath());
                } else {
                    LOGGER.error("was not able to delete; {}", f.getAbsolutePath());
                }
            }
        }

        LOGGER.info("did clear");
    }

}
