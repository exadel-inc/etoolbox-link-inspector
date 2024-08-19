package com.exadel.etoolbox.linkinspector.core.services.mocks;

import com.exadel.etoolbox.contractor.entity.Context;
import com.exadel.etoolbox.contractor.entity.ManagedTask;
import com.exadel.etoolbox.contractor.entity.Task;
import com.exadel.etoolbox.contractor.entity.TaskInfo;
import com.exadel.etoolbox.contractor.service.tasking.Contractor;
import com.exadel.etoolbox.contractor.util.ContractorUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class MockContractor implements Contractor {
    @Override
    public Context newContext(@NotNull String s, @NotNull String s1, String s2) {
        return ContractorUtil.EMPTY_CONTEXT;
    }

    @Override
    public Context newJobContext(@NotNull String s, String s1) {
        return ContractorUtil.EMPTY_CONTEXT;
    }

    @Override
    public Context newJobContext(@NotNull Job job, @NotNull JobExecutionContext jobExecutionContext) {
        return ContractorUtil.EMPTY_CONTEXT;
    }

    @Override
    public @Nullable TaskInfo getInfo(@NotNull String id) {
        return null;
    }

    @Override
    public @NotNull List<TaskInfo> getAllInfo(String s) {
        return Collections.emptyList();
    }

    @Override
    public @NotNull TaskInfo run(@NotNull String topic) {
        return newTaskInfo(topic);
    }

    @Override
    public @NotNull TaskInfo run(@NotNull Task task, String topic, String title) {
        return newTaskInfo(topic);
    }

    @Override
    public @NotNull TaskInfo run(@NotNull ManagedTask managedTask, String topic, String title) {
        return newTaskInfo(topic);
    }

    @Override
    public @NotNull TaskInfo runExclusive(@NotNull String topic) {
        return newTaskInfo(topic);
    }

    @Override
    public @NotNull TaskInfo runExclusive(@NotNull Task task, @NotNull String topic, String title) {
        return newTaskInfo(topic);
    }

    @Override
    public @NotNull TaskInfo runExclusive(@NotNull ManagedTask managedTask, @NotNull String topic, String title) {
        return newTaskInfo(topic);
    }

    @Override
    public TaskInfo discard(@NotNull String id) {
        return TaskInfo.builder().id(id).build();
    }

    @Override
    public List<TaskInfo> discardAll(@NotNull String topic) {
        return Collections.emptyList();
    }

    private static TaskInfo newTaskInfo(String topic) {
        return TaskInfo.builder().topic(topic).build();
    }
}
