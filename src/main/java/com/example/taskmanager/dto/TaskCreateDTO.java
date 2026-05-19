package com.example.taskmanager.dto;

import com.example.taskmanager.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateDTO {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    private Task.TaskStatus status;
}
