package org.haiku.haikudepotserver.job.model;

public class JobRunnerException extends Exception {

    public JobRunnerException(String message) {
        super(message);
    }

    public JobRunnerException(String message, Throwable th) {
        super(message, th);
    }

}
