package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskCreateDTO;
import com.example.taskmanager.dto.TaskDTO;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task task;
    private TaskCreateDTO taskCreateDTO;

    @BeforeEach
    void setUp() {
        task = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(Task.TaskStatus.TODO)
                .createdAt(LocalDateTime.now())
                .build();

        taskCreateDTO = TaskCreateDTO.builder()
                .title("Test Task")
                .description("Test Description")
                .status(Task.TaskStatus.TODO)
                .build();
    }

    @Test
    void getTaskById_shouldReturnTaskDTO() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        TaskDTO result = taskService.getTaskById(1L);

        assertNotNull(result);
        assertEquals(task.getTitle(), result.getTitle());
        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    void createTask_shouldReturnSavedTaskDTO() {
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskDTO result = taskService.createTask(taskCreateDTO);

        assertNotNull(result);
        assertEquals(task.getTitle(), result.getTitle());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void createTask_withNullStatus_shouldUseDefaultTodo() {
        taskCreateDTO.setStatus(null);
        task.setStatus(Task.TaskStatus.TODO);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskDTO result = taskService.createTask(taskCreateDTO);

        assertNotNull(result);
        assertEquals(Task.TaskStatus.TODO, result.getStatus());
        verify(taskRepository).save(argThat(t -> t.getStatus() == Task.TaskStatus.TODO));
    }
}
