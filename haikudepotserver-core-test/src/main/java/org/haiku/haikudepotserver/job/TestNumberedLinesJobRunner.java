/*
 * Copyright 2018-2025, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haiku.haikudepotserver.job;

import com.google.common.net.MediaType;
import com.google.common.util.concurrent.Uninterruptibles;
import org.haiku.haikudepotserver.job.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class TestNumberedLinesJobRunner extends AbstractJobRunner<TestNumberedLinesJobSpecification> {

    protected static Logger LOGGER = LoggerFactory.getLogger(TestNumberedLinesJobRunner.class);

    @Override
    public void run(JobService jobService, TestNumberedLinesJobSpecification specification) throws IOException, JobRunnerException {

        JobDataWithByteSink jobDataWithByteSink = jobService.storeGeneratedData(
                specification.getGuid(),
                "download",
                MediaType.PLAIN_TEXT_UTF_8.toString(),
                JobDataEncoding.NONE);

        try (
                OutputStream outputStream = jobDataWithByteSink.getByteSink().openStream();
                Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        ) {
            for(int i = 0; i < specification.getLines(); i++) {
                LOGGER.info("written line {}", i);
                writer.append(Integer.toString(i));
                writer.append('\n');
                Uninterruptibles.sleepUninterruptibly(specification.getDelayPerLineMillis(), TimeUnit.MILLISECONDS);
            }
        }

    }
}
