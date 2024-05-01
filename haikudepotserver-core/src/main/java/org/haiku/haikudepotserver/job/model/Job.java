/*
 * Copyright 2014-2016, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haiku.haikudepotserver.job.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

/**
 * <p>The job run state is used to convey data about the running of a job; has it started, was it cancelled etc...</p>
 */

public class Job implements Comparable<JobSnapshot>, JobSnapshot {

    private Date startTimestamp;
    private Date finishTimestamp;
    private Date queuedTimestamp;
    private Date failTimestamp;
    private Date cancelTimestamp;
    private Integer progressPercent;

    /**
     * <p>This is the {@link JobSpecification} that this instance is
     * conveying the run state for.</p>
     */

    private JobSpecification jobSpecification;

    private Set<String> generatedDataGuids = Sets.newHashSet();

    public Job() {
        super();
    }

    public Job(JobSnapshot other) {
        this();
        Preconditions.checkArgument(null != other, "the other job run state must be supplied");
        this.startTimestamp = other.getStartTimestamp();
        this.finishTimestamp = other.getFinishTimestamp();
        this.queuedTimestamp = other.getQueuedTimestamp();
        this.failTimestamp = other.getFailTimestamp();
        this.cancelTimestamp = other.getCancelTimestamp();
        this.progressPercent = other.getProgressPercent();
        this.jobSpecification = other.getJobSpecification();
        this.generatedDataGuids = Sets.newHashSet(other.getGeneratedDataGuids());
    }

    public Job(JobSpecification jobSpecification) {
        this();
        this.jobSpecification = jobSpecification;
    }

    @Override
    public Date getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(Date startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public void setStartTimestamp() {
        setStartTimestamp(new Date());
    }

    @Override
    public Date getFinishTimestamp() {
        return finishTimestamp;
    }

    public void setFinishTimestamp(Date finishTimestamp) {
        this.finishTimestamp = finishTimestamp;
    }

    public void setFinishTimestamp() {
        setFinishTimestamp(new Date());
    }

    @Override
    public Date getQueuedTimestamp() {
        return queuedTimestamp;
    }

    public void setQueuedTimestamp(Date queuedTimestamp) {
        this.queuedTimestamp = queuedTimestamp;
    }

    public void setQueuedTimestamp() {
        setQueuedTimestamp(new Date());
    }

    @Override
    public Date getFailTimestamp() {
        return failTimestamp;
    }

    public void setFailTimestamp(Date failTimestamp) {
        this.failTimestamp = failTimestamp;
    }

    public void setFailTimestamp() {
        setFailTimestamp(new Date());
    }

    @Override
    public Date getCancelTimestamp() {
        return cancelTimestamp;
    }

    public void setCancelTimestamp(Date cancelTimestamp) {
        this.cancelTimestamp = cancelTimestamp;
    }

    public void setCancelTimestamp() {
        setCancelTimestamp(new Date());
    }

    @Override
    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    @Override
    public JobSpecification getJobSpecification() {
        return jobSpecification;
    }

    public void setJobSpecification(JobSpecification jobSpecification) {
        this.jobSpecification = jobSpecification;
    }

    @Override
    public Set<String> getGeneratedDataGuids() {
        return ImmutableSet.copyOf(generatedDataGuids);
    }

    public void addGeneratedDataGuid(String guid) {
        generatedDataGuids.add(guid);
    }

    @Override
    public Set<String> getDataGuids() {
        Set<String> result = Sets.newHashSet();
        result.addAll(generatedDataGuids);
        result.addAll(getJobSpecification().getSuppliedDataGuids());
        return result;
    }

    @Override
    public Status getStatus() {

        if(null!=getCancelTimestamp()) {
            return Status.CANCELLED;
        }

        if(null!=getFailTimestamp()) {
            return Status.FAILED;
        }

        if(null!=getFinishTimestamp()) {
            return Status.FINISHED;
        }

        if(null!=getStartTimestamp()) {
            return Status.STARTED;
        }

        if(null!=getQueuedTimestamp()) {
            return Status.QUEUED;
        }

        return Status.INDETERMINATE;
    }

    public boolean isQueuedOrStarted() {
        return switch (getStatus()) {
            case QUEUED, STARTED -> true;
            default -> false;
        };

    }

    @Override
    public String getOwnerUserNickname() {
        if(null!=jobSpecification) {
            return jobSpecification.getOwnerUserNickname();
        }

        return null;
    }

    @Override
    public String getJobTypeCode() {
        if(null!=jobSpecification) {
            return jobSpecification.getJobTypeCode();
        }

        return null;
    }

    @Override
    public String getGuid() {
        if (null != jobSpecification) {
            return jobSpecification.getGuid();
        }

        return null;
    }

    @Override
    public Optional<Long> tryGetTimeToLiveMillis() {
        if (null != jobSpecification) {
            return jobSpecification.tryGetTimeToLiveMillis();
        }

        return Optional.empty();
    }

    @Override
    public int compareTo(JobSnapshot o) {
        Date qThis = getQueuedTimestamp();
        Date qOther = o.getQueuedTimestamp();
        int cmp = Long.compare(
                null == qOther ? 0 : qOther.getTime(),
                null == qThis ? 0 : qThis.getTime()
        );

        if(0==cmp) {
            cmp = getGuid().compareTo(o.getGuid());
        }

        return cmp;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("guid", getGuid())
                .append("jobTypeCode", getJobTypeCode())
                .append("status", getStatus())
                .build();
    }

}
